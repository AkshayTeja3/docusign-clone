package com.docusign.docusign.dto.response;

import com.docusign.docusign.domain.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DocumentResponse {
    private UUID id;
    private String fileName;
    private String filePath;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private String uploadedBy;


}
