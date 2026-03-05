package com.servicehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String action;
    private String entityType;
    private String entityId;
    private UUID actorId;
    private String actorRole;
    private String actorLocation;
    private Object oldValue;
    private Object newValue;
    private String ipAddress;
    private String correlationId;
    private String description;
    private LocalDateTime createdAt;
}
