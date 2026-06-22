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
    private Instant createdAt;
    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
        this.status = SignatureRequestStatus.PENDING;
    }
    @OneToMany(mappedBy = "signatureRequest", fetch = FetchType.LAZY)
    private List<Signer> signers = new ArrayList<>();

    // 🎯 Paste this method inside your SignatureRequest class
    public void executeSigning(UUID signerId) {
        if (this.status == SignatureRequestStatus.COMPLETED) {
            throw new IllegalStateException("Architectural Violation: This document lifecycle is already finalized.");
        }

        // Find the specific signer trying to sign this document
        Signer targetSigner = this.signers.stream()
                .filter(s -> s.getId().equals(signerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Signer not associated with this request."));

        // Validate sequential signing order rules
        validateSigningOrder(targetSigner);

        // Update the individual signer's state
        targetSigner.setStatus(SignerStatus.SIGNED); // Ensure Signer entity has setStatus or a rich state update method

        // Recalculate if the entire document aggregate is complete
        boolean allSigned = this.signers.stream()
                .allMatch(s -> s.getStatus() == SignerStatus.SIGNED);

        if (allSigned) {
            this.status = SignatureRequestStatus.COMPLETED;
        }
    }

    // Helper method to enforce workflow sequencing
    private void validateSigningOrder(Signer currentSigner) {
        for (Signer s : this.signers) {
            // If someone before them in the order list hasn't signed yet, block the execution!
            if (s.getSigningOrder() < currentSigner.getSigningOrder() && s.getStatus() != SignerStatus.SIGNED) {
                throw new IllegalStateException("It is not this signer's turn yet in the workflow hierarchy.");
            }
        }
    }




}
