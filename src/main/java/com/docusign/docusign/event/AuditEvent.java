package com.docusign.docusign.event;

import com.docusign.docusign.domain.AuditAction;
import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuditEvent {
    private final AuditAction action;
    private final User performedBy;
    private final SignatureRequest signatureRequest;
    private final String details;
}