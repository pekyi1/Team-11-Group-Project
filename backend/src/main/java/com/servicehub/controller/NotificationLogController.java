package com.servicehub.controller;

import com.servicehub.service.NotificationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notification-logs")
@RequiredArgsConstructor
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    @GetMapping
    public ResponseEntity<?> getNotificationLogs(Pageable pageable,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String eventType,
                                                  @RequestParam(required = false) UUID recipientId) {
        return ResponseEntity.ok(notificationLogService.getNotificationLogs(pageable, status, eventType, recipientId));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<?> retryNotification(@PathVariable Long id) {
        return ResponseEntity.ok(notificationLogService.retryNotification(id));
    }
}
