package com.docusign.docusign.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;        // hashed, never plain text

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;          // SENDER or SIGNER

    @Column(nullable = false)
    private Boolean isVerified;     // the "validated" flag you mentioned

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum UserRole {
        SENDER,
        SIGNER
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();  // auto set the time they registered
        this.isVerified = false;  // every new user starts as NOT verified

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email; // Spring uses this as the identifier
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
