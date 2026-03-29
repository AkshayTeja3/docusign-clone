package com.docusign.docusign.config;

import com.docusign.docusign.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Grab the Authorization header from the request
        String authHeader = request.getHeader("Authorization");

        // 2. If no header or doesn't start with "Bearer " → skip, move to next filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract token by removing "Bearer " prefix (7 characters)
        String token = authHeader.substring(7);

        // 4. Extract email from token
        String email = jwtService.extractEmail(token);

        // 5. If email found and user not already authenticated
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // 7. Validate token
            if (jwtService.isTokenValid(token, (User) userDetails)) {

                // 8. Create authentication object
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // 9. Add request details to auth token
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 10. Set authentication in Spring Security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 11. Move to next filter
        filterChain.doFilter(request, response);
    }
}