package com.servicehub.service;

import com.servicehub.exception.SlaPolicyNotFoundException;
import com.servicehub.model.Category;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.Priority;
import com.servicehub.repository.SlaPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for tracking SLA deadlines and compliance.
 * Handles initialization of SLA due dates and recording of response/resolution timestamps.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaTrackingService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final SlaPolicyService slaPolicyService;

    /**
     * Initializes SLA tracking for a newly created service request.
     * Fetches the SLA policy for the request's category and priority,
     * then calculates response and resolution due dates.
     *
     * @param request the service request to initialize SLA for
     * @throws SlaPolicyNotFoundException if no active SLA policy exists for the category+priority combination
     */
    @Transactional
    public void initializeSla(ServiceRequest request) {
        Category category = request.getCategory();
        Priority priority = request.getPriority();
        LocalDateTime createdAt = request.getCreatedAt() != null 
                ? request.getCreatedAt() 
                : LocalDateTime.now();

        try {
            // Fetch the SLA policy using the service lookup method
            SlaPolicy policy = slaPolicyService.getTargetTimes(category, priority);

            // Calculate due dates based on hours from creation time
            LocalDateTime responseDueAt = createdAt.plusHours(policy.getResponseTimeHours());
            LocalDateTime resolutionDueAt = createdAt.plusHours(policy.getResolutionTimeHours());

            // Set the due dates on the request
            request.setResponseDueAt(responseDueAt);
            request.setResolutionDueAt(resolutionDueAt);
            request.setIsSlaBreached(false); // Initialize as not breached

            log.debug("SLA initialized for request {}: response due at {}, resolution due at {}",
                    request.getReferenceNumber(), responseDueAt, resolutionDueAt);
        } catch (SlaPolicyNotFoundException e) {
            log.warn("No SLA policy found for category {} with priority {}. SLA tracking will not be initialized for request {}",
                    category.getName(), priority, request.getReferenceNumber());
            // Set due dates to null to indicate no SLA policy exists
            request.setResponseDueAt(null);
            request.setResolutionDueAt(null);
            request.setIsSlaBreached(false);
            // Note: We don't throw here to allow request creation even without SLA policy
            // The business logic can decide if this should be a hard requirement
        }
    }

    /**
     * Records the response timestamp when a ticket is assigned.
     * Sets responded_at to the current time and checks if the response SLA was met.
     *
     * @param request the service request being assigned
     */
    @Transactional
    public void recordResponse(ServiceRequest request) {
        // Only record if not already recorded
        if (request.getRespondedAt() != null) {
            log.debug("Response already recorded for request {}", request.getReferenceNumber());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        request.setRespondedAt(now);

        // Check if response SLA was met
        if (request.getResponseDueAt() != null) {
            boolean responseSlaMet = now.isBefore(request.getResponseDueAt()) || now.isEqual(request.getResponseDueAt());
            request.setResponseSlaMet(responseSlaMet);

            if (!responseSlaMet) {
                log.warn("Response SLA breached for request {}: responded at {} but due at {}",
                        request.getReferenceNumber(), now, request.getResponseDueAt());
                // Update breach flag if response was missed
                request.setIsSlaBreached(true);
            } else {
                log.debug("Response SLA met for request {}: responded at {} (due at {})",
                        request.getReferenceNumber(), now, request.getResponseDueAt());
            }
        } else {
            // No SLA policy, so we can't determine if SLA was met
            request.setResponseSlaMet(null);
            log.debug("No response SLA deadline set for request {}", request.getReferenceNumber());
        }
    }

    /**
     * Records the resolution timestamp when a ticket is resolved.
     * Sets resolved_at to the current time, checks if the resolution SLA was met,
     * and finalizes the is_sla_breached flag.
     *
     * @param request the service request being resolved
     */
    @Transactional
    public void recordResolution(ServiceRequest request) {
        // Only record if not already recorded
        if (request.getResolvedAt() != null) {
            log.debug("Resolution already recorded for request {}", request.getReferenceNumber());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        request.setResolvedAt(now);

        // Check if resolution SLA was met
        if (request.getResolutionDueAt() != null) {
            boolean resolutionSlaMet = now.isBefore(request.getResolutionDueAt()) || now.isEqual(request.getResolutionDueAt());
            request.setResolutionSlaMet(resolutionSlaMet);

            if (!resolutionSlaMet) {
                log.warn("Resolution SLA breached for request {}: resolved at {} but due at {}",
                        request.getReferenceNumber(), now, request.getResolutionDueAt());
            } else {
                log.debug("Resolution SLA met for request {}: resolved at {} (due at {})",
                        request.getReferenceNumber(), now, request.getResolutionDueAt());
            }
        } else {
            // No SLA policy, so we can't determine if SLA was met
            request.setResolutionSlaMet(null);
            log.debug("No resolution SLA deadline set for request {}", request.getReferenceNumber());
        }

        // Finalize the breach flag: true if either response or resolution SLA was missed
        boolean isBreached = Boolean.TRUE.equals(request.getIsSlaBreached()) 
                || (request.getResponseSlaMet() != null && !request.getResponseSlaMet())
                || (request.getResolutionSlaMet() != null && !request.getResolutionSlaMet());
        request.setIsSlaBreached(isBreached);

        if (isBreached) {
            log.warn("SLA breach finalized for request {}: responseSlaMet={}, resolutionSlaMet={}",
                    request.getReferenceNumber(), request.getResponseSlaMet(), request.getResolutionSlaMet());
        }
    }
}

