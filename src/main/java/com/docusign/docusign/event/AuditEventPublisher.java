package com.docusign.docusign.event;

import com.docusign.docusign.domain.AuditAction;
import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(AuditAction action, User performedBy,
                        SignatureRequest signatureRequest, String details) {
        eventPublisher.publishEvent(new AuditEvent(action, performedBy, signatureRequest, details));
    }
}