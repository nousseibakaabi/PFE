package com.example.back.service;

import com.example.back.entity.ERole;
import com.example.back.entity.User;
import com.example.back.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserContextService {

    private final UserRepository userRepository;

    public UserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        throw new RuntimeException("User not authenticated");
    }

    public boolean isAdmin() {
        User user = getCurrentUser();
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN);
    }


    public boolean isDecideur() {
        User user = getCurrentUser();
        return user.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_DECIDEUR);
    }

}
