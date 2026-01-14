package com.example.back.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        logger.error("Unauthorized error: {}", authException.getMessage());

        // Check the path to determine if it's an error endpoint
        String path = request.getRequestURI();

        if (path.contains("/error")) {
            // For error endpoint, don't require authentication
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            errorDetails.put("error", "Internal Server Error");
            errorDetails.put("message", "An error occurred");
            errorDetails.put("path", path);

            String jsonResponse = convertMapToJson(errorDetails);
            response.getWriter().write(jsonResponse);
            return;
        }

        if (path.contains("/auth/login")) {
            // For login endpoint, return a more specific message
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("success", false);
            errorDetails.put("message", "Invalid username or password");
            errorDetails.put("error", "Authentication Failed");
            errorDetails.put("path", path);

            String jsonResponse = convertMapToJson(errorDetails);
            response.getWriter().write(jsonResponse);
        } else {
            // For other endpoints, return generic unauthorized
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("success", false);
            errorDetails.put("message", "Full authentication is required to access this resource");
            errorDetails.put("error", "Unauthorized");
            errorDetails.put("path", path);

            String jsonResponse = convertMapToJson(errorDetails);
            response.getWriter().write(jsonResponse);
        }
    }

    private String convertMapToJson(Map<String, Object> map) {
        // Simple JSON conversion
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"error\":\"JSON conversion failed\"}";
        }
    }
}