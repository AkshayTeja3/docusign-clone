package com.docusign.docusign.repository;

import com.docusign.docusign.domain.Notification;
import com.docusign.docusign.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    List<Notification> findByRecipientAndIsRead(User recipient, Boolean isRead);
}