package com.ota.integration.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ota.integration.model.NormalizedHotel;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Adapter for the (fictional) "Acme" OTA. Acme returns a nested {@code properties}
 * document with its own field names ({@code propertyId}, {@code sleeps},
 * {@code left}), which this adapter normalizes into the unified model.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AcmeOtaAdapter implements ProviderAdapter {

    private final ObjectMapper objectMapper;

    @Value("${ota.providers.acme.simulate-failure:false}")
    private boolean simulateFailure;

    private final Resource inventory = new ClassPathResource("fixtures/acme-inventory.json");

    @Override
    public String getName() {
        return "acme";
    }

    @Override
    public List<NormalizedHotel> fetchAndNormalize() {
        if (simulateFailure) {
            throw new ProviderException("Acme OTA is currently unreachable (simulated)");
        }
        try (InputStream in = inventory.getInputStream()) {
            AcmeInventory doc = objectMapper.readValue(in, new TypeReference<>() {});
            return doc.properties().stream().map(this::toHotel).toList();
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("Failed to read Acme inventory: " + e.getMessage(), e);
        }
    }

    private NormalizedHotel toHotel(AcmeProperty p) {
        List<NormalizedHotel.NormalizedRoomType> rooms = p.rooms().stream().map(r -> {
            List<NormalizedHotel.NormalizedRate> rates = r.rates().stream().map(rd -> new NormalizedHotel.NormalizedRate(
                    LocalDate.parse(rd.date()), rd.amount(), rd.currency(), rd.left())).toList();
            return new NormalizedHotel.NormalizedRoomType(r.type(), r.label(), r.sleeps(), r.count(), rates);
        }).toList();
        return new NormalizedHotel("acme", p.propertyId(), p.title(),
                p.location().city(), p.location().country(), p.location().address(),
                p.stars(), rooms);
    }

    private record AcmeInventory(List<AcmeProperty> properties) {
    }

    private record AcmeProperty(String propertyId, String title, AcmeLocation location,
                                int stars, List<AcmeRoom> rooms) {
    }

    private record AcmeLocation(String city, String country, String address) {
    }

    private record AcmeRoom(String type, String label, int sleeps, int count, List<AcmeRate> rates) {
    }

    private record AcmeRate(String date, BigDecimal amount, String currency, int left) {
    }
}
