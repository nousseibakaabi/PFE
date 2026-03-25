package com.example.back.security.jwt;

import com.example.back.security.services.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TwoFactorAuthenticationFilter extends OncePerRequestFilter {

    private static final String TWO_FACTOR_AUTH_HEADER = "X-2FA-Code";
    private static final String TWO_FACTOR_BACKUP_HEADER = "X-2FA-Backup-Code";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Ignorer la route de login et les routes 2FA
        return path.contains("/auth/login") || path.contains("/auth/verify-2fa") || path.contains("/api/2fa/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Skip if not authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if not a UserDetailsImpl instance
        if (!(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check if 2FA is required
        if (userDetails.getTwoFactorEnabled() != null && userDetails.getTwoFactorEnabled()) {

            // Skip if already verified
            if (request.getSession().getAttribute("2FA_VERIFIED") != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Check if request contains 2FA code
            String twoFactorCode = request.getHeader(TWO_FACTOR_AUTH_HEADER);
            String backupCode = request.getHeader(TWO_FACTOR_BACKUP_HEADER);

            // If no code provided, return 401
            if (twoFactorCode == null && backupCode == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "TwoFactorRequired");
                errorResponse.put("message", "Two-factor authentication is required");
                errorResponse.put("requiresTwoFactor", true);

                new ObjectMapper().writeValue(response.getWriter(), errorResponse);
                return;
            }

            // Store verification in session
            request.getSession().setAttribute("2FA_VERIFIED", true);
        }

        filterChain.doFilter(request, response);
    }
}