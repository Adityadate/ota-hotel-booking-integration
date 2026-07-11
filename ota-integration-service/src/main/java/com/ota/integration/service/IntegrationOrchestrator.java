package com.ota.integration.service;

import com.ota.common.api.sync.DeadLetterView;
import com.ota.common.api.sync.ReplayResult;
import com.ota.common.api.sync.SyncRequest;
import com.ota.common.api.sync.SyncRunResult;
import com.ota.common.api.sync.SyncRunResult.ProviderResult;
import com.ota.common.exception.Exceptions;
import com.ota.integration.domain.DeadLetter;
import com.ota.integration.domain.DeadLetter.Status;
import com.ota.integration.model.NormalizedHotel;
import com.ota.integration.provider.ProviderAdapter;
import com.ota.integration.repository.DeadLetterRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The integration pipeline. For each provider it:
 *  <ol>
 *    <li>fetches + normalizes inventory (retried inside {@link ProviderCallService});</li>
 *    <li>wraps the whole provider in a Resilience4j circuit breaker so a sick
 *        provider fails fast instead of stacking up threads;</li>
 *    <li>puserts each hotel into the hotel service (retried);</li>
 *    <li>parks any total failure in the dead-letter table for replay.</li>
 *  </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationOrchestrator {

    private final List<ProviderAdapter> adapters;
    private final ProviderCallService providerCallService;
    private final DeadLetterRepository deadLetterRepository;
    private final CircuitBreakerFactory circuitBreakerFactory;

    @Value("${ota.deadletter.max-replays:5}")
    private int maxReplays;

    @Value("${ota.deadletter.replay.enabled:false}")
    private boolean replayEnabled;

    public SyncRunResult runSync(SyncRequest request) {
        List<ProviderAdapter> selected = selectAdapters(request);
        String runId = UUID.randomUUID().toString();
        OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);
        Instant start = Instant.now();

        List<ProviderResult> results = new ArrayList<>();
        int failedProviders = 0;
        for (ProviderAdapter adapter : selected) {
            CircuitBreaker cb = circuitBreakerFactory.create("provider");
            ProviderResult pr;
            try {
                pr = cb.run(() -> processProvider(adapter, runId),
                        t -> failedResult(adapter, runId, t));
            } catch (Throwable t) {
                pr = failedResult(adapter, runId, t);
            }
            if (!"SUCCESS".equals(pr.getStatus())) {
                failedProviders++;
            }
            results.add(pr);
        }

        OffsetDateTime finished = OffsetDateTime.now(ZoneOffset.UTC);
        int totalHotels = results.stream().mapToInt(ProviderResult::getFetched).sum();
        int totalRates = results.stream().mapToInt(ProviderResult::getUpserted).sum();

        return SyncRunResult.builder()
                .runId(runId)
                .startedAt(started)
                .finishedAt(finished)
                .durationMillis(start.until(finished.toInstant(), ChronoUnit.MILLIS))
                .totalHotels(totalHotels)
                .totalRates(totalRates)
                .failedProviders(failedProviders)
                .providers(results)
                .build();
    }

    private ProviderResult processProvider(ProviderAdapter adapter, String runId) {
        long start = System.nanoTime();
        List<NormalizedHotel> hotels = providerCallService.fetch(adapter);
        int upserted = 0;
        for (NormalizedHotel hotel : hotels) {
            providerCallService.push(hotel);
            upserted++;
        }
        return ProviderResult.builder()
                .provider(adapter.getName())
                .status("SUCCESS")
                .fetched(hotels.size())
                .normalized(hotels.size())
                .upserted(upserted)
                .durationMillis(java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
                .build();
    }

    private ProviderResult failedResult(ProviderAdapter adapter, String runId, Throwable t) {
        log.warn("Provider {} pipeline failed: {}", adapter.getName(), t.getMessage());
        recordDeadLetter(adapter.getName(), t.toString());
        return ProviderResult.builder()
                .provider(adapter.getName())
                .status("FAILED")
                .fetched(0)
                .normalized(0)
                .upserted(0)
                .durationMillis(0)
                .detail(t.getMessage())
                .build();
    }

    /**
     * Records a failure, coalescing repeats for the same provider so we keep one
     * dead-letter row and just bump its attempt count.
     */
    private void recordDeadLetter(String provider, String error) {
        deadLetterRepository.findTopByProviderAndStatusIn(provider, List.of(Status.PENDING, Status.REPLAYING))
                .ifPresentOrElse(dl -> {
                    dl.setAttempts(dl.getAttempts() + 1);
                    dl.setErrorMessage(truncate(error));
                    if (dl.getAttempts() >= maxReplays) {
                        dl.setStatus(Status.FAILED);
                    }
                    deadLetterRepository.save(dl);
                }, () -> {
                    DeadLetter dl = new DeadLetter();
                    dl.setProvider(provider);
                    dl.setEntityType("PROVIDER_SYNC");
                    dl.setEntityKey(provider);
                    dl.setErrorMessage(truncate(error));
                    dl.setAttempts(1);
                    dl.setStatus(Status.PENDING);
                    deadLetterRepository.save(dl);
                });
    }

    @Scheduled(fixedDelayString = "${ota.deadletter.replay.interval-ms:60000}")
    public void scheduledReplay() {
        if (!replayEnabled) {
            return;
        }
        ReplayResult result = replayAll();
        log.info("Scheduled DLQ replay: {}", result);
    }

    @Transactional
    public ReplayResult replayAll() {
        List<DeadLetter> pending = deadLetterRepository.findByStatusIn(List.of(Status.PENDING, Status.REPLAYING));
        int inspected = pending.size();
        int replayed = 0;
        int succeeded = 0;
        int stillFailing = 0;

        for (DeadLetter dl : pending) {
            if (dl.getAttempts() >= maxReplays) {
                dl.setStatus(Status.FAILED);
                deadLetterRepository.save(dl);
                stillFailing++;
                continue;
            }
            ProviderAdapter adapter = adapterByName(dl.getProvider());
            if (adapter == null) {
                stillFailing++;
                continue;
            }
            dl.setStatus(Status.REPLAYING);
            deadLetterRepository.save(dl);
            replayed++;
            try {
                processProvider(adapter, "replay");
                deadLetterRepository.delete(dl);
                succeeded++;
            } catch (Throwable t) {
                recordDeadLetter(dl.getProvider(), t.toString());
                stillFailing++;
            }
        }
        return ReplayResult.builder()
                .inspected(inspected)
                .replayed(replayed)
                .succeeded(succeeded)
                .stillFailing(stillFailing)
                .note("Replayed dead-letter entries (max attempts " + maxReplays + ")")
                .build();
    }

    @Transactional
    public ReplayResult replayOne(Long id) {
        DeadLetter dl = deadLetterRepository.findById(id)
                .orElseThrow(() -> Exceptions.notFound("Dead-letter entry %s not found", id));
        ProviderAdapter adapter = adapterByName(dl.getProvider());
        if (adapter == null) {
            throw Exceptions.badRequest("No adapter registered for provider " + dl.getProvider());
        }
        dl.setStatus(Status.REPLAYING);
        deadLetterRepository.save(dl);
        try {
            processProvider(adapter, "replay");
            deadLetterRepository.delete(dl);
            return ReplayResult.builder().inspected(1).replayed(1).succeeded(1).stillFailing(0)
                    .note("Entry " + id + " replayed successfully").build();
        } catch (Throwable t) {
            recordDeadLetter(dl.getProvider(), t.toString());
            return ReplayResult.builder().inspected(1).replayed(1).succeeded(0).stillFailing(1)
                    .note("Entry " + id + " still failing: " + t.getMessage()).build();
        }
    }

    @Transactional(readOnly = true)
    public List<DeadLetterView> listDeadLetters() {
        return deadLetterRepository.findAll().stream()
                .map(dl -> DeadLetterView.builder()
                        .id(dl.getId())
                        .provider(dl.getProvider())
                        .entityType(dl.getEntityType())
                        .entityKey(dl.getEntityKey())
                        .errorMessage(dl.getErrorMessage())
                        .attempts(dl.getAttempts())
                        .status(dl.getStatus().name())
                        .firstSeenAt(OffsetDateTime.ofInstant(dl.getFirstSeenAt(), ZoneOffset.UTC))
                        .lastErrorAt(OffsetDateTime.ofInstant(dl.getLastErrorAt(), ZoneOffset.UTC))
                        .build())
                .toList();
    }

    private List<ProviderAdapter> selectAdapters(SyncRequest request) {
        if (request == null || request.getProviders() == null || request.getProviders().isEmpty()) {
            return List.copyOf(adapters);
        }
        return adapters.stream()
                .filter(a -> request.getProviders().contains(a.getName()))
                .toList();
    }

    private ProviderAdapter adapterByName(String name) {
        return adapters.stream().filter(a -> a.getName().equals(name)).findFirst().orElse(null);
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 1024 ? s.substring(0, 1024) : s;
    }
}
