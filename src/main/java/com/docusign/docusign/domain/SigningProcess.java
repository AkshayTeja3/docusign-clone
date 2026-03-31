package com.docusign.docusign.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signing_process")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SigningProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "signer_id", nullable = false)
    private Signer signer;

    @ManyToOne
    @JoinColumn(name = "signature_request_id", nullable = false)
    private SignatureRequest signatureRequest;

    @Column(nullable = false)
    private LocalDateTime signedAt;

    @Column(nullable = false)
    private String ipAddress;
}
