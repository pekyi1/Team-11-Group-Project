package com.servicehub.controller;

import com.servicehub.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<?> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<?> getAuditLogsByEntity(@PathVariable String entityType,
                                                  @PathVariable String entityId,
                                                  Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByEntity(entityType, entityId, pageable));
    }
}
