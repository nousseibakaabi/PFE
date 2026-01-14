package com.example.back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get current directory
        String currentDir = System.getProperty("user.dir");
        Path uploadPath = Paths.get(currentDir, "uploads");

        System.out.println("Configuring static resources from: " + uploadPath.toAbsolutePath());

        // For Windows: use file:/// prefix
        // For Linux/Mac: use file: prefix
        String os = System.getProperty("os.name").toLowerCase();
        String filePrefix = os.contains("win") ? "file:///" : "file:";

        String resourceLocation = filePrefix + uploadPath.toAbsolutePath() + "/";

        // Serve uploaded files
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0); // No cache for development

        // Optionally serve API endpoint for files (alternative approach)
        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(resourceLocation);
    }
}