package com.ota.integration.provider;

import com.ota.integration.model.NormalizedHotel;
import java.util.List;

/**
 * A connector to one external OTA. Implementations know how to talk to a
 * specific (and typically badly documented) vendor API and normalize the
 * response into {@link NormalizedHotel}s.
 */
public interface ProviderAdapter {

    /** Stable id, e.g. "acme", "globex". Used as the circuit-breaker key. */
    String getName();

    /** Pull inventory and normalize it. Throws {@link ProviderException} on failure. */
    List<NormalizedHotel> fetchAndNormalize();
}
