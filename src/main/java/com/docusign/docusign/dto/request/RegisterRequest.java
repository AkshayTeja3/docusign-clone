package com.docusign.docusign.dto.request;

import com.docusign.docusign.domain.User;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private User.UserRole role;
}