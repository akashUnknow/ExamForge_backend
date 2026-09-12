package com.examforge;

import org.junit.jupiter.api.Test;

/**
 * Verifies the full Spring context - including Flyway migrations against
 * a real Postgres container - starts up cleanly.
 */
class ExamForgeApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // If the context fails to start (bad config, failed migration,
        // missing bean, etc.) this test fails automatically.
    }
}
