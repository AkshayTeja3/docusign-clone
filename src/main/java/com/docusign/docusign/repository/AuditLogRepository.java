package com.docusign.docusign.repository;

import com.docusign.docusign.domain.AuditLog;
import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findBySignatureRequestOrderByTimestampAsc(SignatureRequest signatureRequest);
    List<AuditLog> findByPerformedBy(User user);
}