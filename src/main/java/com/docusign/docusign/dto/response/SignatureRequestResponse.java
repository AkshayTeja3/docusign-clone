package com.docusign.docusign.dto.response;

import com.docusign.docusign.domain.SignatureRequestStatus;
import com.docusign.docusign.domain.SigningType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureRequestResponse {
    private UUID id ;
    private UUID documentId;
    private String senderName;
    private SignatureRequestStatus status;
    private SigningType signingType;
    private List<SignerResponse> signers;
    private LocalDateTime signedAt;

}
