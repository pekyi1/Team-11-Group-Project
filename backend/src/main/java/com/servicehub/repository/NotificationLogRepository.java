package com.servicehub.repository;

import com.servicehub.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByEventId(String eventId);
    List<NotificationLog> findByStatusAndRetryCountLessThan(String status, int maxRetries);
    Page<NotificationLog> findByRecipientId(UUID recipientId, Pageable pageable);
    Page<NotificationLog> findByRelatedEntityTypeAndRelatedEntityId(String entityType, String entityId, Pageable pageable);
}
