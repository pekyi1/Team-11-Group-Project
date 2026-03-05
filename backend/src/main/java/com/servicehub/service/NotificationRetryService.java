package com.servicehub.service;

import com.servicehub.model.NotificationLog;
import com.servicehub.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRetryService {

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void retryFailedNotifications() {
        List<NotificationLog> failedLogs = notificationLogRepository
            .findByStatusAndRetryCountLessThan("FAILED", 3);
        List<NotificationLog> retryLogs = notificationLogRepository
            .findByStatusAndRetryCountLessThan("RETRY", 3);

        failedLogs.addAll(retryLogs);

        for (NotificationLog logEntry : failedLogs) {
            log.info("Retrying notification {} (attempt {})", logEntry.getEventId(), logEntry.getRetryCount() + 1);
            notificationService.retryNotification(logEntry);
        }
    }
}
