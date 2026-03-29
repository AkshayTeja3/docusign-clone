package com.docusign.docusign.controller;


import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.request.DocumentUploadRequest;
import com.docusign.docusign.dto.request.LoginRequest;
import com.docusign.docusign.dto.request.RegisterRequest;
import com.docusign.docusign.dto.response.AuthResponse;
import com.docusign.docusign.dto.response.DocumentResponse;
import com.docusign.docusign.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(documentService.uploadDocument(file, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(documentService.getUserDocuments(user));
    }
}