package com.footwork.api.filter;

import com.footwork.api.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private ApplicationContext applicationContext;

    @Autowired
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        
        // Skip JWT validation for auth endpoints (they handle their own token validation)
        if (requestURI.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            
            try {
                // Check if this is an access token (not a refresh token)
                if (jwtService.isRefreshToken(token)) {
                    System.out.println("JWT Filter - Rejected refresh token for API access: " + requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Access tokens only. Refresh tokens cannot be used for API access.");
                    return;
                }
                
                // Check if this is actually an access token
                if (!jwtService.isAccessToken(token)) {
                    System.out.println("JWT Filter - Rejected invalid token type for API access: " + requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token type. Only access tokens are accepted for API access.");
                    return;
                }
                
                email = jwtService.extractUsername(token);
                System.out.println("JWT Filter - Request: " + requestURI + ", Email: " + email);
                
            } catch (Exception e) {
                System.out.println("JWT Filter - Token parsing error: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token format");
                return;
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Get UserDetailsService from Spring context when needed
                UserDetailsService userDetailsService = applicationContext.getBean(UserDetailsService.class);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                System.out.println("JWT Filter - User authorities: " + userDetails.getAuthorities());
                
                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("JWT Filter - Authentication set successfully");
                } else {
                    System.out.println("JWT Filter - Token validation failed");
                }
            } catch (Exception e) {
                System.out.println("JWT Filter - Error: " + e.getMessage());
            }
        } else if (email == null) {
            System.out.println("JWT Filter - No email extracted from token");
        } else {
            System.out.println("JWT Filter - Authentication already exists");
        }
        filterChain.doFilter(request, response);
    }

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}