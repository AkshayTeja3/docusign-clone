package com.docusign.docusign.dto.response;

import com.docusign.docusign.domain.AuditAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private AuditAction action;
    private String performedBy;
    private String details;
    private LocalDateTime timestamp;
}