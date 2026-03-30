package com.docusign.docusign.dto.response;

import com.docusign.docusign.domain.SignerStatus;
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
public class SignerResponse {
    private UUID id ;
    private String userName;
    private SignerStatus status;
    private Integer signingOrder;
    private LocalDateTime signedAt;
}
