package com.docusign.docusign.service;

import com.docusign.docusign.domain.*;
import com.docusign.docusign.dto.request.SignatureRequestCreate;
import com.docusign.docusign.dto.response.SignatureRequestResponse;
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
public class SignatureRequestService {

    private final SignatureRequestRepository signatureRequestRepository;
    private final SignerRepository signerRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public SignatureRequestResponse createSignatureRequest(
            SignatureRequestCreate request, User sender) {

        // 1. Check document exists
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // 2. Check sender is the one who uploaded it
        if (!document.getUploadedBy().getId().equals(sender.getId())) {
            throw new RuntimeException("You can only send your own documents");
        }

        // 3. Create and save SignatureRequest
        SignatureRequest signatureRequest = SignatureRequest.builder()
                .document(document)
                .sender(sender)
                .signingType(request.getSigningType())
                .build();

        signatureRequestRepository.save(signatureRequest);

        // 4. Create and save each Signer
        List<Signer> signers = request.getSigners().stream()
                .map(signerRequest -> {
                    User user = userRepository.findById(signerRequest.getUserId())
                            .orElseThrow(() -> new RuntimeException("Signer not found"));
                    return Signer.builder()
                            .signatureRequest(signatureRequest)
                            .user(user)
                            .signingOrder(signerRequest.getSigningOrder())
                            .build();
                })
                .toList();

        signerRepository.saveAll(signers);

// ✅ use signers (entity list) not request.getSigners() (DTO list)
        signers.forEach(signer ->
                auditEventPublisher.publish(
                        AuditAction.DOCUMENT_SENT,
                        signer.getUser(),
                        signatureRequest,           // ✅ signatureRequest, not request (that's your DTO)
                        "Signature request sent to " + signer.getUser().getName()
                )
        );

// 5. Return response
        return mapToResponse(signatureRequest, signers);
    }

    public SignatureRequestResponse getSignatureRequest(UUID id) {
        SignatureRequest signatureRequest = signatureRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Signature request not found"));

        List<Signer> signers = signerRepository.findBySignatureRequest(signatureRequest);

        return mapToResponse(signatureRequest, signers);
    }

    public List<SignatureRequestResponse> getUserSignatureRequests(User sender) {
        return signatureRequestRepository.findBySender(sender)
                .stream()
                .map(signatureRequest -> {
                    List<Signer> signers = signerRepository.findBySignatureRequest(signatureRequest);
                    return mapToResponse(signatureRequest, signers);
                })
                .toList();
    }

    // Helper method to avoid repeating mapping logic
    private SignatureRequestResponse mapToResponse(SignatureRequest signatureRequest, List<Signer> signers) {
        return SignatureRequestResponse.builder()
                .id(signatureRequest.getId())
                .documentId(signatureRequest.getDocument().getId())
                .senderName(signatureRequest.getSender().getName())
                .status(signatureRequest.getStatus())
                .signingType(signatureRequest.getSigningType())
                .signedAt(signatureRequest.getCreatedAt())
                .signers(signers.stream()
                        .map(signer -> SignerResponse.builder()
                                .id(signer.getId())
                                .userName(signer.getUser().getName())
                                .status(signer.getStatus())
                                .signingOrder(signer.getSigningOrder())
                                .signedAt(signer.getSignedAt())
                                .build()
                        )
                        .toList()
                )
                .build();
    }
}