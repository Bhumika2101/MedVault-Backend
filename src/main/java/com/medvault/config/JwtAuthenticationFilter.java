package com.medvault. config;

import com.medvault.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http. HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.lang.NonNull;
import org. springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core. context.SecurityContextHolder;
import org.springframework.security.core. userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip JWT validation for public endpoints
        String requestPath = request.getRequestURI();
        log.debug("Processing request: {} {}", request.getMethod(), requestPath);

        if (requestPath.startsWith("/api/auth/")) {
            log.debug("Skipping JWT validation for public auth endpoint");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header, continue with filter chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No valid Authorization header found for: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            log.debug("JWT token found for user: {}", userEmail);

            // If we successfully extracted the username and no authentication exists
            if (userEmail != null && SecurityContextHolder.getContext(). getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                log.debug("User authorities: {}", userDetails.getAuthorities());

                // Validate the token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("✓ User authenticated successfully: {} with roles: {}",
                            userEmail, userDetails.getAuthorities());
                } else {
                    log.warn("✗ Invalid JWT token for user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            // Log the error but don't throw - let Spring Security handle it
            log.error("✗ JWT Token validation error: {}", e.getMessage());
            // Clear any partial authentication
            SecurityContextHolder.clearContext();
        }

        // Always continue with the filter chain
        filterChain.doFilter(request, response);
    }
}