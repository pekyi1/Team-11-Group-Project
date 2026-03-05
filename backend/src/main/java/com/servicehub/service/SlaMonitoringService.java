package com.servicehub.service;

import com.servicehub.event.ServiceRequestEvent;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.NotificationLogRepository;
import com.servicehub.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaMonitoringService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 300000) // every 5 minutes
    @Transactional
    public void checkSlaCompliance() {
        LocalDateTime now = LocalDateTime.now();
        List<RequestStatus> activeStatuses = List.of(RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS);

        List<ServiceRequest> activeRequests = serviceRequestRepository.findAll().stream()
            .filter(r -> activeStatuses.contains(r.getStatus()))
            .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
            .toList();

        for (ServiceRequest request : activeRequests) {
            checkResponseSla(request, now);
            checkResolutionSla(request, now);
        }
    }

    private void checkResponseSla(ServiceRequest request, LocalDateTime now) {
        if (request.getResponseSlaDeadline() == null || request.getRespondedAt() != null) return;

        // Check breach
        if (now.isAfter(request.getResponseSlaDeadline())) {
            if (request.getResponseSlaMet() == null || request.getResponseSlaMet()) {
                request.setResponseSlaMet(false);
                serviceRequestRepository.save(request);

                String eventId = "SLA_BREACHED:" + request.getId() + ":response";
                if (!notificationLogRepository.existsByEventId(eventId)) {
                    eventPublisher.publishEvent(new ServiceRequestEvent(
                        this, request, "SLA_BREACHED", null, "Response SLA breached"));
                    log.warn("Response SLA breached for request {}", request.getReferenceNumber());
                }
            }
            return;
        }

        // Check 75% warning
        long totalMinutes = ChronoUnit.MINUTES.between(request.getCreatedAt(), request.getResponseSlaDeadline());
        long elapsedMinutes = ChronoUnit.MINUTES.between(request.getCreatedAt(), now);
        if (totalMinutes > 0 && elapsedMinutes >= (totalMinutes * 75 / 100)) {
            String eventId = "SLA_WARNING:" + request.getId() + ":response";
            if (!notificationLogRepository.existsByEventId(eventId)) {
                eventPublisher.publishEvent(new ServiceRequestEvent(
                    this, request, "SLA_WARNING", null, "Response SLA 75% consumed"));
                log.info("Response SLA warning for request {}", request.getReferenceNumber());
            }
        }
    }

    private void checkResolutionSla(ServiceRequest request, LocalDateTime now) {
        if (request.getResolutionSlaDeadline() == null || request.getResolvedAt() != null) return;

        // Check breach
        if (now.isAfter(request.getResolutionSlaDeadline())) {
            if (request.getResolutionSlaMet() == null || request.getResolutionSlaMet()) {
                request.setResolutionSlaMet(false);
                serviceRequestRepository.save(request);

                String eventId = "SLA_BREACHED:" + request.getId() + ":resolution";
                if (!notificationLogRepository.existsByEventId(eventId)) {
                    eventPublisher.publishEvent(new ServiceRequestEvent(
                        this, request, "SLA_BREACHED", null, "Resolution SLA breached"));
                    log.warn("Resolution SLA breached for request {}", request.getReferenceNumber());
                }
            }
            return;
        }

        // Check 75% warning
        long totalMinutes = ChronoUnit.MINUTES.between(request.getCreatedAt(), request.getResolutionSlaDeadline());
        long elapsedMinutes = ChronoUnit.MINUTES.between(request.getCreatedAt(), now);
        if (totalMinutes > 0 && elapsedMinutes >= (totalMinutes * 75 / 100)) {
            String eventId = "SLA_WARNING:" + request.getId() + ":resolution";
            if (!notificationLogRepository.existsByEventId(eventId)) {
                eventPublisher.publishEvent(new ServiceRequestEvent(
                    this, request, "SLA_WARNING", null, "Resolution SLA 75% consumed"));
                log.info("Resolution SLA warning for request {}", request.getReferenceNumber());
            }
        }
    }
}
