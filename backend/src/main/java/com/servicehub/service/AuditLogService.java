package com.servicehub.service;

import com.servicehub.dto.response.AuditLogResponse;
import com.servicehub.model.AuditLog;
import com.servicehub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String entityType, String entityId, UUID actorId,
                    String actorRole, String actorLocation, String oldValue, String newValue,
                    String ipAddress, String userAgent, String correlationId, String description) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actorId(actorId)
                .actorRole(actorRole)
                .actorLocation(actorLocation)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .correlationId(correlationId)
                .description(description)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} on {} {}", action, entityType, entityId);
    }

    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public Page<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .actorId(log.getActorId())
                .actorRole(log.getActorRole())
                .actorLocation(log.getActorLocation())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .correlationId(log.getCorrelationId())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
