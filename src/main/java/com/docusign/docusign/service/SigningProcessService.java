package com.docusign.docusign.service;

import com.docusign.docusign.domain.*;
import com.docusign.docusign.dto.response.SigningProcessResponse;
import com.docusign.docusign.event.AuditEventPublisher;
import com.docusign.docusign.repository.SignatureRequestRepository;
import com.docusign.docusign.repository.SignerRepository;
import com.docusign.docusign.repository.SigningProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SigningProcessService {

    private final SignerRepository signerRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final SignatureRequestRepository signatureRequestRepository;
    private final SigningProcessRepository signingProcessRepository;
    private final SignerWorkflowService signerWorkflowService;

    @Transactional // ✅ Added to ensure the entire state transition commits as a single atomic unit
    public SigningProcessResponse signDocument(UUID signerId, User user, String ipAddress) {

        // 1. Fetch signer
        Signer signer = signerRepository.findById(signerId)
                .orElseThrow(() -> new RuntimeException("Signer not found"));

        // 2. Authorization — only the assigned user can sign
        if (!signer.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to sign this document");
        }

        // 3. Check already signed or declined
        if (signer.getStatus() != SignerStatus.PENDING) {
            throw new RuntimeException("Document already " + signer.getStatus().name().toLowerCase());
        }

        // 4. Validate parallel / sequential order
        signerWorkflowService.validateSigningOrder(signer);

        // 5. Update signer status
        signer.setStatus(SignerStatus.SIGNED);
        signer.setSignedAt(Instant.now());
        signerRepository.save(signer);

        // 🎯 FIX: Pull the parent request reference directly from the entity layer instead of controller arguments
        SignatureRequest signatureRequest = signer.getSignatureRequest();

        // 6. Record the signing process
        SigningProcess process = SigningProcess.builder()
                .signer(signer)
                .signatureRequest(signatureRequest)
                .signedAt(signer.getSignedAt())
                .ipAddress(ipAddress)
                .build();
        signingProcessRepository.save(process);

        // ✅ Broadcast Signer Action Event
        auditEventPublisher.publish(
                AuditAction.SIGNER_SIGNED,
                user,
                signatureRequest,
                "Document officially signed by " + user.getName() + " from IP: " + ipAddress
        );

        // 7. Check if all signers are done → finalize the request
        checkAndFinalizeRequest(signatureRequest);

        return SigningProcessResponse.builder()
                .signingProcessId(process.getId())
                .signerId(signer.getId())
                .signerName(signer.getUser().getName())
                .signatureRequestId(signatureRequest.getId())
                .signedAt(process.getSignedAt())
                .ipAddress(process.getIpAddress())
                .build();
    }

    // Finalize document if all signers have signed
    private void checkAndFinalizeRequest(SignatureRequest request) {
        boolean allSigned = request.getSigners()
                .stream()
                .allMatch(s -> s.getStatus() == SignerStatus.SIGNED);

        // 🎯 FIX: Moved inside the condition block so it only publishes when the contract is truly closed!
        if (allSigned) {
            request.setStatus(SignatureRequestStatus.COMPLETED);
            signatureRequestRepository.save(request);

            auditEventPublisher.publish(
                    AuditAction.REQUEST_COMPLETED,
                    request.getSender(),
                    request,
                    "All signers completed. Document finalized."
            );
        }
    }
}