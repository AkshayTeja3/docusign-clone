package com.docusign.docusign.service;

import com.docusign.docusign.domain.Notification;
import com.docusign.docusign.domain.User;
import com.docusign.docusign.dto.response.NotificationResponse;
import com.docusign.docusign.event.AuditEvent;
import com.docusign.docusign.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        String message = resolveMessage(event);

        // only notify if we have a meaningful message
        if (message == null) return;

        User recipient = resolveRecipient(event);

        Notification notification = Notification.builder()
                .recipient(recipient)
                .message(message)
                .build();

        notificationRepository.save(notification);
    }

    // Build the message based on action type
    private String resolveMessage(AuditEvent event) {
        String signerName = event.getPerformedBy().getName();
        return switch (event.getAction()) {
            case DOCUMENT_SENT -> "You have a new document to sign.";
            case SIGNER_SIGNED -> signerName + " has signed your document.";
            case SIGNER_DECLINED -> signerName + " has declined your document.";
            case REQUEST_COMPLETED -> "All signers completed. Your document is finalized!";
            default -> null; // SIGNER_VIEWED doesn't need a notification
        };
    }

    // Decide who gets notified
    private User resolveRecipient(AuditEvent event) {
        return switch (event.getAction()) {
            // signers get notified when document is sent
            case DOCUMENT_SENT -> event.getPerformedBy();
            // sender gets notified for everything else
            default -> event.getSignatureRequest().getSender();
        };
    }

    // Fetch all notifications for logged-in user
    public List<NotificationResponse> getMyNotifications(User user) {
        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user)
                .stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .isRead(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    // Fetch only unread notifications
    public List<NotificationResponse> getUnreadNotifications(User user) {
        return notificationRepository
                .findByRecipientAndIsRead(user, false)
                .stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .message(n.getMessage())
                        .isRead(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }

    // Mark a single notification as read
    public void markAsRead(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}