package com.docusign.docusign.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ METHOD 1: Handles general runtime business exceptions
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    } // 🎯 This brace was missing/misplaced before! It closes method 1.

    // ✅ METHOD 2: Handles concurrent database write race conditions
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleConcurrencyConflict(ObjectOptimisticLockingFailureException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Conflict detected: The document state was updated by another party. Please refresh and try again.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}