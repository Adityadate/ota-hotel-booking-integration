package com.ota.common.api.sync;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Trigger a synchronization run. If {@code providers} is empty/absent, every
 * registered provider adapter is invoked.
 */
@Value
@Builder
@Jacksonized
public class SyncRequest {

    List<String> providers;
}
