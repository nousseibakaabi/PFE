package com.example.back.config;

import com.example.back.security.jwt.AuthTokenFilter;
import com.example.back.security.services.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/test/all").permitAll()
                        .requestMatchers("/test/public").permitAll()

                        // Allow access to uploaded files without authentication
                        .requestMatchers("/uploads/**").permitAll()

                        // Health check and public APIs
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()

                        // Static resources
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()

                        // Profile endpoints - require authentication
                        .requestMatchers("/profile/me").authenticated()
                        .requestMatchers("/profile/update").authenticated()
                        .requestMatchers("/profile/update-with-avatar").authenticated()
                        .requestMatchers("/profile/upload-avatar").authenticated()



                        // In SecurityConfig.java
                        .requestMatchers("/test/user").hasAnyRole("ADMIN", "COMMERCIAL_METIER", "DECIDEUR", "CHEF_PROJET")
                        .requestMatchers("/test/admin").hasRole("ADMIN")
                        .requestMatchers("/test/commercial").hasRole("COMMERCIAL_METIER")
                        .requestMatchers("/test/decideur").hasRole("DECIDEUR")
                        .requestMatchers("/test/chef-projet").hasRole("CHEF_PROJET")

                        // All other requests need authentication
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfig().corsConfigurationSource();
    }
}