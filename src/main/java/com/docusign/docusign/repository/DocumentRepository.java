package com.docusign.docusign.repository;

import com.docusign.docusign.domain.Document;
import com.docusign.docusign.domain.DocumentStatus;
import com.docusign.docusign.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUploadedBy(User user);
    List<Document>  findByStatus(DocumentStatus status);
}
