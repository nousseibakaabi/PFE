package com.example.back.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestControllerTest {

    private final TestController controller = new TestController();

    @Test
    void allAccess_returnsPublicPayload() {
        Map<String, String> response = controller.allAccess();

        assertThat(response)
                .containsEntry("message", "Public Content.")
                .containsEntry("status", "SUCCESS");
    }

    @Test
    void health_returnsUpStatus() {
        Map<String, Object> response = controller.health();

        assertThat(response)
                .containsEntry("status", "UP")
                .containsEntry("service", "Test Controller");
        assertThat(response).containsKey("timestamp");
    }
}
