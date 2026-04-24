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

    @Test
    void userAccess_returnsExpectedMessage() {
        assertThat(controller.userAccess()).isEqualTo("User Content.");
    }

    @Test
    void adminAccess_returnsExpectedMessage() {
        assertThat(controller.adminAccess()).isEqualTo("Admin Board.");
    }

    @Test
    void commercialAccess_returnsExpectedMessage() {
        assertThat(controller.commercialAccess()).isEqualTo("Commercial Metier Board.");
    }

    @Test
    void decideurAccess_returnsExpectedMessage() {
        assertThat(controller.decideurAccess()).isEqualTo("Decideur Board.");
    }

    @Test
    void chefProjetAccess_returnsExpectedMessage() {
        assertThat(controller.chefProjetAccess()).isEqualTo("Chef de Projet Board.");
    }

    @Test
    void ping_returnsPongPayload() {
        assertThat(controller.ping())
                .containsEntry("message", "pong")
                .containsEntry("status", "OK");
    }
}
