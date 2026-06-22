package com.docusign.docusign.service;

import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.event.DocumentSignedEvent;
import com.docusign.docusign.repository.SignatureRequestRepository;
import com.docusign.docusign.exception.ResourceNotFoundException; // Ensure this matches your custom exception package
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 🎯 FIXED: Swapped to Spring's Transactional annotation

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SigningService {
    private final SignatureRequestRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional // ✅ Now perfectly integrated with Spring's Transaction Management Engine
    public void completeSigningFlow(UUID requestId, UUID signerId) {
        SignatureRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        // Execute the business logic inside the entity aggregate root
        request.executeSigning(signerId);
        repository.save(request);

        // This event will now correctly wait for the DB transaction to fully commit before alerting the listener!
        eventPublisher.publishEvent(new DocumentSignedEvent(requestId, signerId));
    }
}