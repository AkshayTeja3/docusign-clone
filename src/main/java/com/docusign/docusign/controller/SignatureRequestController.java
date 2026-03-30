package com.docusign.docusign.controller;

import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.request.SignatureRequestCreate;
import com.docusign.docusign.dto.response.DocumentResponse;
import com.docusign.docusign.dto.response.SignatureRequestResponse;
import com.docusign.docusign.service.SignatureRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/signature-requests")
@RequiredArgsConstructor
public class SignatureRequestController {

    private final SignatureRequestService signatureRequestService;

    @PostMapping
    public ResponseEntity<SignatureRequestResponse> createSignatureRequest(
            @RequestBody SignatureRequestCreate request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(signatureRequestService.createSignatureRequest(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SignatureRequestResponse> getSignatureRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(signatureRequestService.getSignatureRequest(id));
    }

    @GetMapping
    public ResponseEntity<List<SignatureRequestResponse>> getUserSignatureRequests(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(signatureRequestService.getUserSignatureRequests(user));
    }
}