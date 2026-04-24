package com.example.back.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AvatarService {


    @PostConstruct
    public void init() {
        try {
            // Get current working directory
            String currentDir = System.getProperty("user.dir");
            Path uploadPath = Paths.get(currentDir, "uploads", "avatars");

            Files.createDirectories(uploadPath);
            System.out.println("==========================================");
            System.out.println("Avatar upload directory initialized at: " + uploadPath.toAbsolutePath());
            System.out.println("Directory exists: " + Files.exists(uploadPath));
            System.out.println("==========================================");
        } catch (IOException e) {
            System.err.println("Could not create upload directory: " + e.getMessage());
        }
    }

    public String generateAvatarUrlForSignup(String firstName, String lastName, String username) {
        String initials = generateInitials(firstName, lastName);

        try {
            // Get current working directory
            String currentDir = System.getProperty("user.dir");
            Path avatarPath = Paths.get(currentDir, "uploads", "avatars");

            // Ensure directory exists
            if (!Files.exists(avatarPath)) {
                Files.createDirectories(avatarPath);
                System.out.println("Created directory: " + avatarPath.toAbsolutePath());
            }

            String filename = username + "_avatar.svg";
            Path filePath = avatarPath.resolve(filename);

            System.out.println("Attempting to save avatar to: " + filePath.toAbsolutePath());

            // Create SVG content with thin border, small font, perfect centering
            String svg = String.format(
                    "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
                            "<circle cx='50' cy='50' r='46' fill='none' stroke='#2B72DF' stroke-width='2'/>" +
                            "<text x='50' y='50' text-anchor='middle' font-family='Arial, sans-serif' font-size='36' fill='#2B72DF' font-weight='500' dy='0.35em'>%s</text>" +
                            "</svg>",
                    initials
            );

            // Save SVG file
            Files.write(filePath, svg.getBytes());
            System.out.println("Successfully saved avatar to: " + filePath.toAbsolutePath());
            System.out.println("File exists after save: " + Files.exists(filePath));

            // Return the URL path that will be handled by WebConfig
            String fileUrl = "/uploads/avatars/" + filename;
            System.out.println("Avatar URL to store in DB: " + fileUrl);

            return fileUrl;

        } catch (IOException e) {
            System.err.println("Failed to save avatar: " + e.getMessage());
            // Fallback to Base64
            return generateShortSvgUrl(initials);
        }
    }


    private String generateShortSvgUrl(String initials) {
        // Fallback: Generate Base64 encoded SVG with thin border, small font, perfect centering
        String svg = String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
                        "<circle cx='50' cy='50' r='46' fill='none' stroke='#2B72DF' stroke-width='2'/>" +
                        "<text x='50' y='50' text-anchor='middle' font-family='Arial, sans-serif' font-size='36' fill='#2B72DF' font-weight='500' dy='0.35em'>%s</text>" +
                        "</svg>",
                initials
        );

        String base64 = java.util.Base64.getEncoder().encodeToString(svg.getBytes());
        return "data:image/svg+xml;base64," + base64;
    }

    public String generateInitials(String firstName, String lastName) {
        String firstInitial = "";
        String lastInitial = "";

        if (firstName != null && !firstName.isEmpty()) {
            firstInitial = firstName.substring(0, 1).toUpperCase();
        }

        if (lastName != null && !lastName.isEmpty()) {
            lastInitial = lastName.substring(0, 1).toUpperCase();
        }

        if (firstInitial.isEmpty() && lastInitial.isEmpty()) {
            return "U";
        }

        return firstInitial + lastInitial;
    }
}