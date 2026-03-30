package com.docusign.docusign.repository;

import com.docusign.docusign.domain.Document;
import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.SignatureRequestResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SignatureRequestRepository extends JpaRepository<SignatureRequest, UUID> {
    List<SignatureRequest> findBySender(User sender);
    List<SignatureRequest> findByDocument(Document document);
}
