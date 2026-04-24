package com.example.back.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarControllerTest {

    private final AvatarController controller = new AvatarController();

    @Test
    void uploadAvatar_withRegularFileName_throwsBecauseExtensionParsingIsBroken() {
        MockMultipartFile file = new MockMultipartFile(
                "avatar",
                "avatar.png",
                "image/png",
                "avatar-content".getBytes()
        );

        assertThatThrownBy(() -> controller.uploadAvatar(file))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
    }
}
