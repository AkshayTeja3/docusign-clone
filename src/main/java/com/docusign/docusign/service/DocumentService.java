package com.docusign.docusign.service;

import com.docusign.docusign.domain.Document;
import com.docusign.docusign.domain.SignatureRequest;
import com.docusign.docusign.domain.SignatureRequestStatus;
import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.DocumentResponse;
import com.docusign.docusign.repository.DocumentRepository;
import com.docusign.docusign.repository.SignatureRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final SignatureRequestRepository signatureRequestRepository;

    // method goes here
    public List<DocumentResponse> getUserDocuments(User user) {
        return documentRepository.findByUploadedBy(user)  // gives List<Document>
                .stream()                                  // convert to stream
                .map(document -> DocumentResponse.builder()  // convert each Document to DocumentResponse
                        .id(document.getId())
                        .fileName(document.getFileName())
                        .filePath(document.getFilePath())
                        .status(document.getStatus())
                        .createdAt(Instant.from(document.getCreatedAt()))
                        .uploadedBy(document.getUploadedBy().getName())
                        .build()
                )
                .toList();                                 // convert back to List
    }
    public DocumentResponse uploadDocument(MultipartFile file, User user) throws IOException {
        // 1. Create uploads folder if it doesn't exist
        Path uploadPath = Path.of("uploads");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 2. Generate unique file name to avoid conflicts
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 3. Save file to local storage
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        // 4. Create and save Document record to DB
        Document document = Document.builder()
                .fileName(fileName)
                .filePath(filePath.toString())
                .uploadedBy(user)
                .build();
        documentRepository.save(document);

        // 5. Return response
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .status(document.getStatus())
                .createdAt(Instant.from(document.getCreatedAt()))
                .uploadedBy(user.getName())
                .build();
    }
    public DocumentResponse getDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .status(document.getStatus())
                .createdAt(Instant.from(document.getCreatedAt()))
                .uploadedBy(document.getUploadedBy().getName())
                .build();


    }

    @Transactional
    public void deleteDocument(UUID documentId, User sender) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document target not found"));

        // 🎯 CHAOS TRAP: Check for active signature lifecycles linked to this document
        List<SignatureRequest> activeRequests = signatureRequestRepository.findByDocumentAndStatus(
                document, SignatureRequestStatus.PENDING);

        if (!activeRequests.isEmpty()) {
            throw new IllegalStateException("Cannot delete a document associated with an active, pending signature workflow. Revoke the signature requests first!");
        }

        // Safe to execute hard or soft delete now
        documentRepository.delete(document);
    }

}