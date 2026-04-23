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
        String currentDir = System.getProperty("user.dir");
        Path uploadPath = Paths.get(currentDir, "uploads");

        System.out.println("Configuring static resources from: " + uploadPath.toAbsolutePath());

        String os = System.getProperty("os.name").toLowerCase();
        String filePrefix = os.contains("win") ? "file:///" : "file:";

        String resourceLocation = filePrefix + uploadPath.toAbsolutePath() + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0);

        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(resourceLocation);
    }
}