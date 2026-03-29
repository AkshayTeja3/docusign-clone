package com.docusign.docusign.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable=false)
    private String filePath;
    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private DocumentStatus status;
    @Column(nullable=false)
    private LocalDateTime createdAt;
    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    private String fileName;
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = DocumentStatus.DRAFT;
    }
}
