package com.docusign.docusign.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Signer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "signature_request_id")
    private SignatureRequest signatureRequest;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private SignerStatus status;
    @Column(nullable=false)
    private Integer signingOrder;
    @Column
    private LocalDateTime signedAt;
    @PrePersist
    public void prePersist() {
        this.status = SignerStatus.PENDING;
    }
}
