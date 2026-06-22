package com.docusign.docusign.config;

import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.repository.SignatureRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("documentSecurityEvaluator")
@RequiredArgsConstructor
public class DocumentSecurityEvaluator {

    private final SignatureRequestRepository repository;

    public boolean isParticipant(UUID requestId, String currentUsername) {
        SignatureRequest request = repository.findById(requestId).orElse(null);

        if (request == null) {
            return false; // Document doesn't exist, block access instantly
        }

        // 1. Check if the user is the original sender
        boolean isSender = request.getSender() != null &&
                request.getSender().getUsername().equals(currentUsername);

        // 2. Check if the user matches one of the assigned signers via their linked User account
        boolean isSigner = request.getSigners().stream()
                .anyMatch(signer -> signer.getUser() != null &&
                        signer.getUser().getUsername().equals(currentUsername));

        // Access granted if they are either the sender or an assigned signer
        return isSender || isSigner;
    }
}