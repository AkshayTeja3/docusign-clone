package com.docusign.docusign.repository;

import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.domain.Signer;
import com.docusign.docusign.domain.SigningProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SigningProcessRepository extends JpaRepository<SigningProcess, UUID> {
    List<SigningProcess> findBySignatureRequest(SignatureRequest signatureRequest);
    Optional<SigningProcess> findBySigner(Signer signer);
}