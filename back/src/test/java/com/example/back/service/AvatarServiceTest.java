package com.example.back.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AvatarServiceTest {

    @InjectMocks
    private AvatarService avatarService;

    @Test
    void testGenerateInitials_BothNames() {
        String result = avatarService.generateInitials("John", "Doe");
        assertEquals("JD", result);
    }

    @Test
    void testGenerateInitials_FirstNameOnly() {
        String result = avatarService.generateInitials("John", "");
        assertEquals("J", result);
    }

    @Test
    void testGenerateInitials_LastNameOnly() {
        String result = avatarService.generateInitials("", "Doe");
        assertEquals("D", result);
    }

    @Test
    void testGenerateInitials_NoNames() {
        String result = avatarService.generateInitials("", "");
        assertEquals("U", result);
    }

    @Test
    void testGenerateInitials_NullFirstName() {
        String result = avatarService.generateInitials(null, "Doe");
        assertEquals("D", result);
    }

    @Test
    void testGenerateInitials_NullLastName() {
        String result = avatarService.generateInitials("John", null);
        assertEquals("J", result);
    }

    @Test
    void testGenerateInitials_BothNamesNull() {
        String result = avatarService.generateInitials(null, null);
        assertEquals("U", result);
    }

    @Test
    void testGenerateInitials_LowercaseNames() {
        String result = avatarService.generateInitials("john", "doe");
        assertEquals("JD", result);
    }

    @Test
    void testGenerateInitials_SingleCharacterNames() {
        String result = avatarService.generateInitials("J", "D");
        assertEquals("JD", result);
    }

    @Test
    void testGenerateInitials_NamesWithSpaces() {
        String result = avatarService.generateInitials("John Michael", "Doe Smith");
        assertEquals("JD", result);
    }

    @Test
    void testGenerateInitials_SpecialCharacters() {
        String result = avatarService.generateInitials("Jöhn", "D'Artagnan");
        assertEquals("JD", result);
    }
}