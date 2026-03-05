package com.servicehub.service;

import com.servicehub.event.ServiceRequestEvent;
import com.servicehub.model.NotificationLog;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.repository.NotificationLogRepository;
import com.servicehub.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    @Value("${mail.from:noreply@servicehub.local}")
    private String fromAddress;

    @Value("${mail.enabled:true}")
    private String mailEnabled;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, readOnly = true)
    public void handleServiceRequestEvent(ServiceRequestEvent event) {
        if (!"true".equalsIgnoreCase(mailEnabled)) return;

        // Re-fetch the entity in a new transaction to ensure lazy associations are accessible
        ServiceRequest request = serviceRequestRepository.findById(event.getServiceRequest().getId()).orElse(null);
        if (request == null) return;
        String eventType = event.getEventType();

        switch (eventType) {
            case "REQUEST_CREATED" -> notifyRequestCreated(request, event);
            case "REQUEST_ASSIGNED" -> notifyRequestAssigned(request, event);
            case "STATUS_IN_PROGRESS" -> notifyStatusChange(request, event, "In Progress");
            case "STATUS_RESOLVED" -> notifyStatusChange(request, event, "Resolved");
            case "STATUS_CLOSED" -> notifyStatusChange(request, event, "Closed");
            case "TICKET_TRANSFERRED" -> notifyTransfer(request, event);
            case "REQUEST_REOPENED" -> notifyReopened(request, event);
            case "SLA_WARNING" -> notifySlaWarning(request, event);
            case "SLA_BREACHED" -> notifySlaBreach(request, event);
            default -> log.warn("Unknown event type: {}", eventType);
        }
    }

    private void notifyRequestCreated(ServiceRequest request, ServiceRequestEvent event) {
        User requester = request.getRequester();
        if (requester == null) return;
        String eventId = buildEventId("REQUEST_CREATED", request.getId());
        if (notificationLogRepository.existsByEventId(eventId)) return;

        String subject = String.format("[ServiceHub] Request %s Created — %s",
            request.getReferenceNumber(), request.getCategory().getName());
        String body = String.format("Your service request %s has been created.\n\nTitle: %s\nPriority: %s\nCategory: %s\n\nView: %s/requests/%d",
            request.getReferenceNumber(), request.getTitle(), request.getPriority(),
            request.getCategory().getName(), frontendUrl, request.getId());

        sendAndLog(eventId, "REQUEST_CREATED", requester, subject, body, request);
    }

    private void notifyRequestAssigned(ServiceRequest request, ServiceRequestEvent event) {
        User agent = request.getAssignedAgent();
        if (agent == null) return;
        String eventId = buildEventId("REQUEST_ASSIGNED", request.getId());
        if (notificationLogRepository.existsByEventId(eventId)) return;

        String subject = String.format("[ServiceHub] New Ticket Assigned: %s — %s (%s)",
            request.getReferenceNumber(), request.getCategory().getName(),
            request.getLocation() != null ? request.getLocation().getName() : "");
        String body = String.format("You have been assigned ticket %s.\n\nTitle: %s\nPriority: %s\n\nView: %s/requests/%d",
            request.getReferenceNumber(), request.getTitle(), request.getPriority(), frontendUrl, request.getId());

        sendAndLog(eventId, "REQUEST_ASSIGNED", agent, subject, body, request);
    }

    private void notifyStatusChange(ServiceRequest request, ServiceRequestEvent event, String statusLabel) {
        User requester = request.getRequester();
        if (requester == null) return;
        String eventId = buildEventId("STATUS_" + statusLabel.toUpperCase().replace(" ", "_"), request.getId());
        if (notificationLogRepository.existsByEventId(eventId)) return;

        String subject = String.format("[ServiceHub] %s is now %s", request.getReferenceNumber(), statusLabel);
        String body = String.format("Your service request %s status has changed to %s.\n\nTitle: %s\n\nView: %s/requests/%d",
            request.getReferenceNumber(), statusLabel, request.getTitle(), frontendUrl, request.getId());

        sendAndLog(eventId, "STATUS_" + statusLabel.toUpperCase().replace(" ", "_"), requester, subject, body, request);
    }

    private void notifyTransfer(ServiceRequest request, ServiceRequestEvent event) {
        // Notify new agent
        User newAgent = request.getAssignedAgent();
        if (newAgent != null) {
            String eventId = buildEventId("TICKET_TRANSFERRED_AGENT", request.getId());
            if (!notificationLogRepository.existsByEventId(eventId)) {
                String subject = String.format("[ServiceHub] %s Transferred to You", request.getReferenceNumber());
                String body = String.format("Ticket %s has been transferred to you.\n\nTitle: %s\nReason: %s\n\nView: %s/requests/%d",
                    request.getReferenceNumber(), request.getTitle(), event.getComment(), frontendUrl, request.getId());
                sendAndLog(eventId, "TICKET_TRANSFERRED", newAgent, subject, body, request);
            }
        }
        // Notify requester
        User requester = request.getRequester();
        if (requester != null) {
            String eventId = buildEventId("TICKET_TRANSFERRED_REQUESTER", request.getId());
            if (!notificationLogRepository.existsByEventId(eventId)) {
                String subject = String.format("[ServiceHub] %s Transferred to %s",
                    request.getReferenceNumber(), newAgent != null ? newAgent.getFullName() : "another agent");
                String body = String.format("Your request %s has been transferred.\n\nView: %s/requests/%d",
                    request.getReferenceNumber(), frontendUrl, request.getId());
                sendAndLog(eventId, "TICKET_TRANSFERRED", requester, subject, body, request);
            }
        }
    }

    private void notifyReopened(ServiceRequest request, ServiceRequestEvent event) {
        User agent = request.getAssignedAgent();
        if (agent == null) return;
        String eventId = buildEventId("REQUEST_REOPENED", request.getId());
        if (notificationLogRepository.existsByEventId(eventId)) return;

        String subject = String.format("[ServiceHub] %s Reopened by Requester", request.getReferenceNumber());
        String body = String.format("Ticket %s has been reopened.\n\nTitle: %s\n\nView: %s/requests/%d",
            request.getReferenceNumber(), request.getTitle(), frontendUrl, request.getId());

        sendAndLog(eventId, "REQUEST_REOPENED", agent, subject, body, request);
    }

    private void notifySlaWarning(ServiceRequest request, ServiceRequestEvent event) {
        User agent = request.getAssignedAgent();
        if (agent == null) return;
        String eventId = buildEventId("SLA_WARNING", request.getId());
        if (notificationLogRepository.existsByEventId(eventId)) return;

        String subject = String.format("[ServiceHub] SLA Warning: %s", request.getReferenceNumber());
        String body = String.format("SLA warning for ticket %s — deadline approaching.\n\nTitle: %s\nPriority: %s\n\nView: %s/requests/%d",
            request.getReferenceNumber(), request.getTitle(), request.getPriority(), frontendUrl, request.getId());

        sendAndLog(eventId, "SLA_WARNING", agent, subject, body, request);
    }

    private void notifySlaBreach(ServiceRequest request, ServiceRequestEvent event) {
        User agent = request.getAssignedAgent();
        if (agent == null) return;
        String eventId = buildEventId("SLA_BREACHED", request.getId());
        if (notificationLogRepository.existsByEventId(eventId)) return;

        String subject = String.format("[ServiceHub] SLA Breached: %s", request.getReferenceNumber());
        String body = String.format("SLA has been breached for ticket %s.\n\nTitle: %s\nPriority: %s\n\nView: %s/requests/%d",
            request.getReferenceNumber(), request.getTitle(), request.getPriority(), frontendUrl, request.getId());

        sendAndLog(eventId, "SLA_BREACHED", agent, subject, body, request);
    }

    private String buildEventId(String type, Long requestId) {
        return String.format("%s:%d:%s", type, requestId, LocalDateTime.now().toString());
    }

    private void sendAndLog(String eventId, String eventType, User recipient,
                            String subject, String body, ServiceRequest request) {
        NotificationLog logEntry = NotificationLog.builder()
            .eventType(eventType)
            .eventId(eventId)
            .recipientId(recipient.getId())
            .recipientEmail(recipient.getEmail())
            .subject(subject)
            .status("PENDING")
            .relatedEntityType("SERVICE_REQUEST")
            .relatedEntityId(String.valueOf(request.getId()))
            .build();

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            logEntry.setStatus("SENT");
            logEntry.setSentAt(LocalDateTime.now());
            log.info("Email sent: {} to {}", eventType, recipient.getEmail());
        } catch (Exception e) {
            logEntry.setStatus("FAILED");
            logEntry.setLastError(e.getMessage());
            log.error("Failed to send email: {} to {}: {}", eventType, recipient.getEmail(), e.getMessage());
        }

        notificationLogRepository.save(logEntry);
    }

    // Public method for retry job
    public void retryNotification(NotificationLog logEntry) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(logEntry.getRecipientEmail());
            message.setSubject(logEntry.getSubject());
            message.setText("Retry of notification: " + logEntry.getEventType());
            mailSender.send(message);

            logEntry.setStatus("SENT");
            logEntry.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            logEntry.setRetryCount(logEntry.getRetryCount() + 1);
            logEntry.setLastError(e.getMessage());
            if (logEntry.getRetryCount() >= 3) {
                logEntry.setStatus("FAILED");
            } else {
                logEntry.setStatus("RETRY");
            }
        }
        notificationLogRepository.save(logEntry);
    }
}
