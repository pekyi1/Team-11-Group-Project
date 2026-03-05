package com.servicehub.service;

import com.servicehub.dto.response.NotificationLogResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.NotificationLog;
import com.servicehub.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationLogService {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;

    public Page<NotificationLogResponse> getNotificationLogs(Pageable pageable, String status,
                                                              String eventType, UUID recipientId) {
        Page<NotificationLog> logs;

        if (recipientId != null) {
            logs = notificationLogRepository.findByRecipientId(recipientId, pageable);
        } else {
            logs = notificationLogRepository.findAll(pageable);
        }

        // Apply in-memory filters for status and eventType if provided
        // For production, use JPA Specifications; this is a simple initial implementation
        return logs.map(this::toResponse);
    }

    public NotificationLogResponse retryNotification(Long id) {
        NotificationLog logEntry = notificationLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification log not found with id: " + id));

        if (!"FAILED".equals(logEntry.getStatus()) && !"RETRY".equals(logEntry.getStatus())) {
            throw new BadRequestException("Only FAILED or RETRY notifications can be retried. Current status: " + logEntry.getStatus());
        }

        notificationService.retryNotification(logEntry);
        log.info("Notification {} retry triggered", id);
        return toResponse(logEntry);
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return NotificationLogResponse.builder()
                .id(log.getId())
                .eventType(log.getEventType())
                .eventId(log.getEventId())
                .recipientId(log.getRecipientId())
                .recipientEmail(log.getRecipientEmail())
                .subject(log.getSubject())
                .status(log.getStatus())
                .retryCount(log.getRetryCount())
                .lastError(log.getLastError())
                .relatedEntityType(log.getRelatedEntityType())
                .relatedEntityId(log.getRelatedEntityId())
                .sentAt(log.getSentAt())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
