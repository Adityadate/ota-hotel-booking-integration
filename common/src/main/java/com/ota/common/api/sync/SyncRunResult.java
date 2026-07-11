package com.ota.common.api.sync;

import com.ota.common.api.sync.SyncRunResult.ProviderResult;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SyncRunResult {

    String runId;
    OffsetDateTime startedAt;
    OffsetDateTime finishedAt;
    long durationMillis;
    int totalHotels;
    int totalRates;
    int failedProviders;
    List<ProviderResult> providers;

    @Value
    @Builder
    @Jacksonized
    public static class ProviderResult {

        String provider;
        String status;        // SUCCESS | PARTIAL | FAILED
        int fetched;
        int normalized;
        int upserted;
        int errors;
        long durationMillis;
        String detail;
    }
}
