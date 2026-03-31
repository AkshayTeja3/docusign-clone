package com.docusign.docusign.repository;

import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.domain.Signer;
import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.SignerResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface SignerRepository extends JpaRepository <Signer, UUID>{
    List<Signer> findBySignatureRequest(SignatureRequest signatureRequest);
    Optional<Signer> findBySignatureRequestAndUser(SignatureRequest signatureRequest, User user);
    List<Signer> findByUser(User user);
    Optional<Signer> findBySignatureRequestAndSigningOrder(SignatureRequest signatureRequest, Integer signingOrder);
}
