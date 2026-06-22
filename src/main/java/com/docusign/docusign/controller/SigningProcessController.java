package com.docusign.docusign.controller;

import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.SigningProcessResponse;
import com.docusign.docusign.service.SigningProcessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/signing")
@RequiredArgsConstructor
public class SigningProcessController {

    private final SigningProcessService signingProcessService;

    // 🎯 FIXED: Standardized path mapping to provide BOTH parameters so SpEL can read #requestId safely
    @PostMapping("/requests/{requestId}/signers/{signerId}/sign")
    @PreAuthorize("@documentSecurityEvaluator.isParticipant(#requestId, principal.username)")
    public ResponseEntity<SigningProcessResponse> signDocument(
            @PathVariable UUID requestId,
            @PathVariable UUID signerId,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();
        SigningProcessResponse response = signingProcessService.signDocument(signerId, user, ipAddress);
        return ResponseEntity.ok(response);
    }
}