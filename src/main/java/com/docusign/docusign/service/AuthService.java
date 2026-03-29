package com.docusign.docusign.service;

import com.docusign.docusign.config.JwtService;
import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.request.LoginRequest;
import com.docusign.docusign.dto.request.RegisterRequest;
import com.docusign.docusign.dto.response.AuthResponse;
import com.docusign.docusign.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;


    public AuthResponse register(RegisterRequest request) {
        // 1. Build user object from request
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // hash password
                .role(request.getRole())
                .build();

        // 2. Save to DB
        userRepository.save(user);

        // 3. Generate JWT token
        String token = jwtService.generateToken(user);

        // 4. Return response
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid password or email"));

        // 2. Check password matches
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password or email");
        }

        // 3. Generate JWT token
        String token = jwtService.generateToken(user);

        // 4. Return response
        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

}
