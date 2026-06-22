package com.docusign.docusign.controller;

import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.DocumentResponse;
import com.docusign.docusign.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    // 🎯 REMOVED old getUserDocuments loop completely from here to eliminate the collision

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> getDocuments(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Page<DocumentResponse> page = documentService.getDocumentsForUser(user, pageable);
        return ResponseEntity.ok(page);
    }
}