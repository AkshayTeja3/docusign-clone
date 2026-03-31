package com.docusign.docusign.dto.response;

import lombok.*;
import java.util.*;
import java.time.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
