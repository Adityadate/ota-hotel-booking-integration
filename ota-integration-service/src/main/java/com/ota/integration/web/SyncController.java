package com.ota.integration.web;

import com.ota.common.api.sync.DeadLetterView;
import com.ota.common.api.sync.ReplayResult;
import com.ota.common.api.sync.SyncRequest;
import com.ota.common.api.sync.SyncRunResult;
import com.ota.integration.service.IntegrationOrchestrator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final IntegrationOrchestrator orchestrator;

    /** Trigger a synchronization run. Empty body syncs every registered provider. */
    @PostMapping
    public SyncRunResult trigger(@RequestBody(required = false) SyncRequest request) {
        return orchestrator.runSync(request);
    }

    @GetMapping("/dead-letter")
    public List<DeadLetterView> deadLetters() {
        return orchestrator.listDeadLetters();
    }

    @PostMapping("/dead-letter/replay")
    public ReplayResult replayAll() {
        return orchestrator.replayAll();
    }

    @PostMapping("/dead-letter/{id}/replay")
    @ResponseStatus(HttpStatus.OK)
    public ReplayResult replayOne(@PathVariable Long id) {
        return orchestrator.replayOne(id);
    }
}
