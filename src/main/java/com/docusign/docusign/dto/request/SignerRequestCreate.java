package com.docusign.docusign.dto.request;

import com.docusign.docusign.domain.SigningType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureRequestCreate {
    private UUID documentId;
    private SigningType signingType;
    private List<SignerRequest> signers;
}
