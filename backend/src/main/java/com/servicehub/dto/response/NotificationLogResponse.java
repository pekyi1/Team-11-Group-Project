package com.servicehub.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationLogResponse {
    private Long id;
    private String eventType;
    private String eventId;
    private UUID recipientId;
    private String recipientEmail;
    private String subject;
    private String status;
    private Integer retryCount;
    private String lastError;
    private String relatedEntityType;
    private String relatedEntityId;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
