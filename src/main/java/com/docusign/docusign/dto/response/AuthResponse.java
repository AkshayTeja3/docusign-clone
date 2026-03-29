package com.docusign.docusign.dto.response;


import com.docusign.docusign.domain.User;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;        // JWT token
    private String name;
    private String email;
    private User.UserRole role;
}