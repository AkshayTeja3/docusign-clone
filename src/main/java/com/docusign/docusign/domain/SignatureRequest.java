package com.docusign.docusign.domain;


import jakarta.persistence.*;
import lombok.*;

import java.util.*;
import java.time.*;


@Entity
@Table
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SignatureRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;
    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private SignatureRequestStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private SigningType signingType;
    @Column(nullable=false)
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = SignatureRequestStatus.PENDING;
    }

}
