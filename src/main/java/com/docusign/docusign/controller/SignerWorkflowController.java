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
import org.springframework.security.access.prepost.PreAuthorize; // 🎯 FIXED: Missing Import Added!
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

    // ✅ SECURED: Intercepts the query parameter to validate request access
    @GetMapping("/pending")
    @PreAuthorize("@documentSecurityEvaluator.isParticipant(#requestId, principal.username)")
    public ResponseEntity<List<SignerResponse>> getPendingRequests(
            @RequestParam UUID requestId, // 🎯 FIXED: Added parameter so SpEL can find #requestId
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(signerWorkflowService.getPendingRequests(user));
    }

    // ✅ SECURED: Path includes both requestId and signerId for complete object authorization verification
    @PostMapping("/requests/{requestId}/signers/{signerId}/decline")
    @PreAuthorize("@documentSecurityEvaluator.isParticipant(#requestId, principal.username)")
    public ResponseEntity<SignerResponse> declineRequest(
            @PathVariable UUID requestId, // 🎯 FIXED: Passed via path mapping so SpEL can intercept it
            @PathVariable UUID signerId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(signerWorkflowService.declineRequest(signerId, user));
    }
}