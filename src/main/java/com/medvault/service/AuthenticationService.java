package com.medvault.service;

import com.medvault. model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security. core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final UserService userService;
    
    /**
     * Get the currently authenticated user's email
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext(). getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        return authentication.getName(); // This returns the email/username
    }
    
    /**
     * Get the currently authenticated user entity from database
     */
    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userService.getUserByEmail(email);
    }
    
    /**
     * Get the currently authenticated user's ID
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}