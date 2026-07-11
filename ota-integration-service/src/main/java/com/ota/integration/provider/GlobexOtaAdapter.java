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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapter for the (fictional) "Globex" OTA. Globex uses a completely different
 * document ({@code hotels}, {@code hotelName}, {@code roomTypes}, {@code prices},
 * {@code roomsLeft}) — the point is that two "fragmented" sources converge on one
 * normalized model here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GlobexOtaAdapter implements ProviderAdapter {

    private final ObjectMapper objectMapper;

    @Value("${ota.providers.globex.simulate-failure:false}")
    private boolean simulateFailure;

    private final Resource inventory = new ClassPathResource("fixtures/globex-inventory.json");

    @Override
    public String getName() {
        return "globex";
    }

    @Override
    public List<NormalizedHotel> fetchAndNormalize() {
        if (simulateFailure) {
            throw new ProviderException("Globex OTA returned a 503 (simulated)");
        }
        try (InputStream in = inventory.getInputStream()) {
            GlobexInventory doc = objectMapper.readValue(in, new TypeReference<>() {});
            return doc.hotels().stream().map(this::toHotel).toList();
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("Failed to read Globex inventory: " + e.getMessage(), e);
        }
    }

    private NormalizedHotel toHotel(GlobexHotel h) {
        List<NormalizedHotel.NormalizedRoomType> rooms = h.roomTypes().stream().map(rt -> {
            List<NormalizedHotel.NormalizedRate> rates = rt.prices().stream().map(pr -> new NormalizedHotel.NormalizedRate(
                    LocalDate.parse(pr.night()), pr.price(), pr.cur(), pr.roomsLeft())).toList();
            return new NormalizedHotel.NormalizedRoomType(rt.roomCode(), rt.name(), rt.maxGuests(), rt.inventory(), rates);
        }).toList();
        return new NormalizedHotel("globex", h.code(), h.hotelName(),
                h.cityName(), h.nation(), null, h.rating(), rooms);
    }

    private record GlobexInventory(List<GlobexHotel> hotels) {
    }

    private record GlobexHotel(String code, String hotelName, String cityName, String nation,
                               int rating, List<GlobexRoomType> roomTypes) {
    }

    private record GlobexRoomType(String roomCode, String name, int maxGuests, int inventory,
                                  List<GlobexPrice> prices) {
    }

    private record GlobexPrice(String night, BigDecimal price, String cur, int roomsLeft) {
    }
}
