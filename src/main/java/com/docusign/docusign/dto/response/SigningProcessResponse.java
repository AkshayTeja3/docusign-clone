package com.docusign.docusign.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SigningProcessResponse {
    private UUID signingProcessId;
    private UUID signerId;
    private String signerName;
    private UUID signatureRequestId;
    private LocalDateTime signedAt;
    private String ipAddress;
}