/*package com.example.backend.service;

import com.example.backend.entity.User;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AvatarService {

    private final String AVATAR_DIR = "avatars/";


    public String generateAvatarUrl(User user) {
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            return user.getProfileImage();
        }

        String initials = generateInitials(user.getFirstName(), user.getLastName());
        return generateShortSvgUrl(initials);
    }


    public String generateAvatarUrlForSignup(String firstName, String lastName, String username) {
        String initials = generateInitials(firstName, lastName);
        String filename = username + "_avatar.svg";
        Path filePath = Paths.get(AVATAR_DIR, filename);

        try {
            Files.createDirectories(filePath.getParent());
            String svg = String.format(
                    "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
                            "<circle cx='50' cy='50' r='48' fill='#4F46E5'/>" +
                            "<text x='50' y='58' text-anchor='middle' font-family='Arial' font-size='38' fill='white'>%s</text>" +
                            "</svg>",
                    initials
            );
            Files.write(filePath, svg.getBytes());
            return "/avatars/" + filename;  // Return the path instead of Base64
        } catch (IOException e) {
            // Fallback to initials
            return generateShortSvgUrl(initials);
        }
    }

    private String generateShortSvgUrl(String initials) {
        // Use a simpler, more compact SVG
        String svg = String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
                        "<circle cx='50' cy='50' r='48' fill='#4F46E5'/>" +
                        "<text x='50' y='58' text-anchor='middle' font-family='Arial,sans-serif' font-size='38' fill='white' font-weight='bold'>%s</text>" +
                        "</svg>",
                initials
        );

        // Base64 encode
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

 */


package com.example.back.service;

import com.example.back.entity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AvatarService {


    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${server.url:http://localhost:8081}")
    private String serverUrl;

    @PostConstruct
    public void init() {
        try {
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir, "avatars");
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not create upload directory: " + e.getMessage());
        }
    }

    public String generateAvatarUrlForSignup(String firstName, String lastName, String username) {
        String initials = generateInitials(firstName, lastName);

        try {
            String filename = username + "_avatar.svg";
            Path avatarPath = Paths.get(uploadDir, "avatars");

            // Ensure directory exists
            if (!Files.exists(avatarPath)) {
                Files.createDirectories(avatarPath);
            }

            // Create SVG content
            String svg = String.format(
                    "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
                            "<circle cx='50' cy='50' r='48' fill='#4F46E5'/>" +
                            "<text x='50' y='58' text-anchor='middle' font-family='Arial' font-size='38' fill='white'>%s</text>" +
                            "</svg>",
                    initials
            );

            // Save SVG file
            Path filePath = avatarPath.resolve(filename);
            Files.write(filePath, svg.getBytes());

            // Return FULL ABSOLUTE URL
            String fileUrl = serverUrl + "/uploads/avatars/" + filename;
            System.out.println("Generated avatar URL: " + fileUrl);

            return fileUrl;

        } catch (IOException e) {
            // Fallback to Base64
            return generateShortSvgUrl(initials);
        }
    }

    // Update the upload method too
    private String saveAvatarFile(MultipartFile file, String username) throws IOException {
        String uploadDir = "uploads/avatars";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = username + "_" + UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        // Return FULL ABSOLUTE URL
        return serverUrl + "/uploads/avatars/" + newFilename;
    }

    private String generateShortSvgUrl(String initials) {
        // Fallback: Generate Base64 encoded SVG
        String svg = String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>" +
                        "<circle cx='50' cy='50' r='48' fill='#4F46E5'/>" +
                        "<text x='50' y='58' text-anchor='middle' font-family='Arial,sans-serif' font-size='38' fill='white' font-weight='bold'>%s</text>" +
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