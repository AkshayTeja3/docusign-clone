package com.docusign.docusign.service;

import com.docusign.docusign.domain.AuditLog;
import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.dto.response.AuditLogResponse;
import com.docusign.docusign.event.AuditEvent;
import com.docusign.docusign.repository.AuditLogRepository;
import com.docusign.docusign.repository.SignatureRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SignatureRequestRepository signatureRequestRepository;

    // Listens to all AuditEvents and saves them to DB
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        AuditLog log = AuditLog.builder()
                .action(event.getAction())
                .performedBy(event.getPerformedBy())
                .signatureRequest(event.getSignatureRequest())
                .details(event.getDetails())
                .build();
        auditLogRepository.save(log);
    }

    // Fetch full audit trail for a signature request
    public List<AuditLogResponse> getLogsForRequest(UUID signatureRequestId) {
        SignatureRequest request = signatureRequestRepository.findById(signatureRequestId)
                .orElseThrow(() -> new RuntimeException("Signature request not found"));

        return auditLogRepository
                .findBySignatureRequestOrderByTimestampAsc(request)
                .stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .action(log.getAction())
                        .performedBy(log.getPerformedBy().getName())
                        .details(log.getDetails())
                        .timestamp(log.getTimestamp())
                        .build())
                .toList();
    }
}