package com.docusign.docusign.service;


import com.docusign.docusign.domain.*;
import com.docusign.docusign.dto.response.SignerResponse;
import com.docusign.docusign.event.AuditEventPublisher;
import com.docusign.docusign.repository.DocumentRepository;
import com.docusign.docusign.repository.SignatureRequestRepository;
import com.docusign.docusign.repository.SignerRepository;
import com.docusign.docusign.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class SignerWorkflowService {

    private final SignerRepository signerRepository;
    private final AuditEventPublisher auditEventPublisher;

    private final SignatureRequestRepository signatureRequestRepository;

    // 1. Get all pending requests for a signer
    public List<SignerResponse> getPendingRequests(User user) {
        return signerRepository.findByUser(user)
                .stream()
                .filter(signer -> signer.getStatus() == SignerStatus.PENDING)
                .map(signer -> SignerResponse.builder()
                        .id(signer.getId())
                        .userName(signer.getUser().getName())
                        .status(signer.getStatus())
                        .signingOrder(signer.getSigningOrder())
                        .signedAt(signer.getSignedAt())
                        .build()
                )
                .toList();
    }

    // 2. Validate signing order - internal helper
    public void validateSigningOrder(Signer signer) {
        SignatureRequest request = signer.getSignatureRequest();

        // PARALLEL → everyone can sign anytime, no check needed
        if (request.getSigningType() == SigningType.PARALLEL) {
            return;
        }

        // SEQUENTIAL → check if previous signer has signed
        if (request.getSigningType() == SigningType.SEQUENTIAL) {
            int currentOrder = signer.getSigningOrder();

            // if first signer → no previous signer to check
            if (currentOrder == 1) return;

            // find previous signer
            Signer previousSigner = signerRepository
                    .findBySignatureRequestAndSigningOrder(request, currentOrder - 1)
                    .orElseThrow(() -> new RuntimeException("Previous signer not found"));

            // if previous signer hasn't signed yet → block
            if (previousSigner.getStatus() != SignerStatus.SIGNED) {
                throw new RuntimeException("Waiting for previous signer to sign first");
            }
        }
    }

    // 3. Decline a request
    @Transactional
    public SignerResponse declineRequest(UUID signerId, User user) {
        Signer signer = signerRepository.findById(signerId)
                .orElseThrow(() -> new RuntimeException("Signer not found"));

        // 1. Authorization Guard (Your excellent work)
        if (!signer.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to decline this request");
        }

        // 2. 🎯 CHAOS TRAP: Defend against the Infinite Rejection Loop!
        if (signer.getStatus() == SignerStatus.DECLINED) {
            throw new IllegalStateException("This signing request has already been declined.");
        }
        if (signer.getStatus() == SignerStatus.SIGNED) {
            throw new IllegalStateException("Cannot decline a document that has already been legally signed.");
        }

        // 3. Mutate internal state safely
        signer.setStatus(SignerStatus.DECLINED);

        // 4. Update the overall signature request status securely
        if (signer.getSignatureRequest() != null) {
            signer.getSignatureRequest().setStatus(SignatureRequestStatus.DECLINED);
            // NOTE: signatureRequestRepository.save() removed. Hibernate handles this automatically via dirty checking!
        }

        // 5. Fire the audit event ONCE (Safe from event spamming now!)
        auditEventPublisher.publish(
                AuditAction.SIGNER_DECLINED,
                user,
                signer.getSignatureRequest(),
                signer.getUser().getName() + " declined the signing request"
        );

        // NOTE: signerRepository.save() removed. Managed entities flush changes automatically on commit.
        return SignerResponse.builder()
                .id(signer.getId())
                .userName(signer.getUser().getName())
                .status(signer.getStatus())
                .signingOrder(signer.getSigningOrder())
                .signedAt(signer.getSignedAt())
                .build();
    }
}