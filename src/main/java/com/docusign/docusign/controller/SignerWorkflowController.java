package com.docusign.docusign.controller;

import com.docusign.docusign.domain.Signer;
import com.docusign.docusign.domain.SignerStatus;
import com.docusign.docusign.domain.User;
import com.docusign.docusign.repository.*;
import com.docusign.docusign.dto.response.SignerResponse;
import com.docusign.docusign.service.SignerWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/signer-workflow")
@RequiredArgsConstructor

public class SignerWorkflowController {

    private final SignerWorkflowService signerWorkflowService;

    @GetMapping("/pending")
    public ResponseEntity<List<SignerResponse>> getPendingRequests(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(signerWorkflowService.getPendingRequests(user));
    }

    @PostMapping("/{signerId}/decline")
    public ResponseEntity<SignerResponse> declineRequest(
            @PathVariable UUID signerId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(signerWorkflowService.declineRequest(signerId, user));
    }
}