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
    public SignerResponse declineRequest(UUID signerId, User user) {
        Signer signer = signerRepository.findById(signerId)
                .orElseThrow(() -> new RuntimeException("Signer not found"));

        // make sure this user is the actual signer
        if (!signer.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to decline this request");
        }

        // change status to declined
        signer.setStatus(SignerStatus.DECLINED);
        signerRepository.save(signer);

        // also update the overall signature request status
        signer.getSignatureRequest().setStatus(SignatureRequestStatus.DECLINED);
        signatureRequestRepository.save(signer.getSignatureRequest());

        auditEventPublisher.publish(
                AuditAction.SIGNER_DECLINED,
                user,
                signer.getSignatureRequest(),
                signer.getUser().getName() + " declined the signing request"
        );

        return SignerResponse.builder()
                .id(signer.getId())
                .userName(signer.getUser().getName())
                .status(signer.getStatus())
                .signingOrder(signer.getSigningOrder())
                .signedAt(signer.getSignedAt())
                .build();
    }
}