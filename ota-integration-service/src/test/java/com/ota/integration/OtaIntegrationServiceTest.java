package com.ota.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.ota.common.api.sync.DeadLetterView;
import com.ota.common.api.sync.SyncRunResult;
import com.ota.integration.client.HotelUpsertClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class OtaIntegrationServiceTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @MockBean
    HotelUpsertClient hotelUpsertClient;

    private String base() {
        return "http://localhost:" + port + "/api/v1/sync";
    }

    @Test
    void syncsAllProvidersThenDeadLettersOnFailure() {
        // Phase 1: hotel service reachable -> both providers succeed.
        ResponseEntity<SyncRunResult> ok = rest.postForEntity(base(), null, SyncRunResult.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        SyncRunResult result = ok.getBody();
        assertThat(result.getProviders()).hasSize(2);
        assertThat(result.getFailedProviders()).isZero();
        assertThat(result.getTotalHotels()).isEqualTo(4); // 2 Acme + 2 Globex
        assertThat(rest.getForObject(base() + "/dead-letter", DeadLetterView[].class)).isEmpty();

        // Phase 2: hotel service starts failing -> pipeline dead-letters the failure.
        doThrow(new RuntimeException("hotel-service down"))
                .when(hotelUpsertClient).upsertHotels(any());

        ResponseEntity<SyncRunResult> failed = rest.postForEntity(base(), null, SyncRunResult.class);
        assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(failed.getBody().getFailedProviders()).isGreaterThanOrEqualTo(1);

        DeadLetterView[] dlq = rest.getForObject(base() + "/dead-letter", DeadLetterView[].class);
        assertThat(dlq).isNotEmpty();
        assertThat(dlq[0].getStatus()).isIn("PENDING", "FAILED");
    }
}
