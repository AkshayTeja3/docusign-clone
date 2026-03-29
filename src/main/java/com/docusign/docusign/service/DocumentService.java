package com.docusign.docusign.service;

import com.docusign.docusign.domain.Document;
import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.DocumentResponse;
import com.docusign.docusign.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    // method goes here
    public List<DocumentResponse> getUserDocuments(User user) {
        return documentRepository.findByUploadedBy(user)  // gives List<Document>
                .stream()                                  // convert to stream
                .map(document -> DocumentResponse.builder()  // convert each Document to DocumentResponse
                        .id(document.getId())
                        .fileName(document.getFileName())
                        .filePath(document.getFilePath())
                        .status(document.getStatus())
                        .createdAt(document.getCreatedAt())
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
                .createdAt(document.getCreatedAt())
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
                .createdAt(document.getCreatedAt())
                .uploadedBy(document.getUploadedBy().getName())
                .build();
    }

}