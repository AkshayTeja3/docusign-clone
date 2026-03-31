package com.docusign.docusign.controller;

import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.AuditLogResponse;
import com.docusign.docusign.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/audit/{signatureRequestId}
     * Returns full audit trail for a signature request (sender only)
     */
    @GetMapping("/{signatureRequestId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(
            @PathVariable UUID signatureRequestId,
            @AuthenticationPrincipal User user) {

        List<AuditLogResponse> logs = auditLogService.getLogsForRequest(signatureRequestId);
        return ResponseEntity.ok(logs);
    }
}