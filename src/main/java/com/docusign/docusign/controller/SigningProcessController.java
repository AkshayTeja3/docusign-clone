package com.docusign.docusign.controller;

import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.SigningProcessResponse;
import com.docusign.docusign.service.SigningProcessService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/signing")
@RequiredArgsConstructor
public class SigningProcessController {

    private final SigningProcessService signingProcessService;

    /**
     * POST /api/signing/{signerId}/sign
     * Captures IP from request, validates order, records signing process
     */
    @PostMapping("/{signerId}/sign")
    public ResponseEntity<SigningProcessResponse> signDocument(
            @PathVariable UUID signerId,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        String ipAddress = request.getRemoteAddr();
        SigningProcessResponse response = signingProcessService.signDocument(signerId, user, ipAddress);
        return ResponseEntity.ok(response);
    }
}