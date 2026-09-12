package com.examforge.common.controller;

import com.examforge.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SystemControllerTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void systemInfo_returnsStandardSuccessEnvelope() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/system/info", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"success\":true");
        assertThat(response.getBody()).contains("ExamForge");
        assertThat(response.getBody()).contains("\"timestamp\"");
    }

    @Test
    void unknownRoute_whenUnauthenticated_returnsStandardErrorEnvelope() {
        // Security now runs before MVC dispatch, so an unauthenticated
        // request to any non-public route (known or unknown) is rejected
        // with 401 before Spring ever gets a chance to 404 it. The 404
        // path (NoResourceFoundException) is still exercised by
        // unknownPublicRoute_returns404 below via a whitelisted prefix.
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/does-not-exist", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).contains("\"success\":false");
    }

    @Test
    void unknownPublicRoute_returns404() {
        // /api/v1/system/** is public, so a bogus sub-path under it clears
        // security and reaches Spring MVC's routing, which 404s cleanly.
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/system/does-not-exist", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("\"success\":false");
    }

    @Test
    void healthEndpoint_isUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("UP");
    }
}
