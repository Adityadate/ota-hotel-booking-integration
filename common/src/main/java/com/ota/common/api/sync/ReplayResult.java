package com.ota.common.api.sync;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ReplayResult {

    int inspected;
    int replayed;
    int succeeded;
    int stillFailing;
    String note;
}
