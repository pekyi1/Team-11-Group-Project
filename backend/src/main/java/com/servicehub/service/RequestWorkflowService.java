package com.servicehub.service;

import com.servicehub.dto.response.StatusUpdateResponse;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.StatusHistory;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.StatusHistoryRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing service request status workflow transitions.
 * Handles status updates with role-based validation and audit trail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RequestWorkflowService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final StatusTransitionValidator statusTransitionValidator;

    /**
     * Updates the status of a service request.
     * Validates the transition based on current status, target status, and user role.
     * Creates an audit trail entry in status_history.
     *
     * @param requestId  the ID of the service request
     * @param newStatus  the target status
     * @param comment    optional comment for the status change
     * @return StatusUpdateResponse containing the update details
     * @throws ResourceNotFoundException if request or user not found
     * @throws com.servicehub.exception.InvalidStatusTransitionException if transition is invalid
     */
    @Transactional
    public StatusUpdateResponse updateStatus(Long requestId, RequestStatus newStatus, String comment) {
        // Fetch the request
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service request not found with id: " + requestId));

        // Get current authenticated user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + userEmail));

        Role userRole = currentUser.getRole();
        RequestStatus currentStatus = request.getStatus();

        // Validate the transition
        statusTransitionValidator.validateTransition(currentStatus, newStatus, userRole);

        // Update the request status
        String previousStatus = currentStatus.name();
        request.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        // Handle SLA tracking based on status changes
        updateSlaTracking(request, newStatus, now);

        // Save the updated request
        ServiceRequest savedRequest = serviceRequestRepository.save(request);

        // Create status history entry
        StatusHistory statusHistory = StatusHistory.builder()
                .request(savedRequest)
                .fromStatus(previousStatus)
                .toStatus(newStatus.name())
                .changedBy(currentUser)
                .comment(comment)
                .build();
        StatusHistory savedHistory = statusHistoryRepository.save(statusHistory);

        log.info("Status updated: Request {} transitioned from {} to {} by {} ({})",
                savedRequest.getReferenceNumber(), previousStatus, newStatus, currentUser.getFullName(), userRole);

        // Build and return response
        return new StatusUpdateResponse(
                savedRequest.getId(),
                savedRequest.getReferenceNumber(),
                previousStatus,
                newStatus.name(),
                currentUser.getId(),
                currentUser.getFullName(),
                comment,
                savedHistory.getChangedAt()
        );
    }

    /**
     * Updates SLA tracking fields based on status changes.
     *
     * @param request   the service request
     * @param newStatus the new status
     * @param now       the current timestamp
     */
    private void updateSlaTracking(ServiceRequest request, RequestStatus newStatus, LocalDateTime now) {
        // When status moves to IN_PROGRESS, mark response SLA
        if (newStatus == RequestStatus.IN_PROGRESS && request.getRespondedAt() == null) {
            request.setRespondedAt(now);
            if (request.getResponseSlaDeadline() != null) {
                request.setResponseSlaMet(now.isBefore(request.getResponseSlaDeadline()));
            }
        }

        // When status moves to RESOLVED, mark resolution SLA
        if (newStatus == RequestStatus.RESOLVED) {
            request.setResolvedAt(now);
            if (request.getResolutionSlaDeadline() != null) {
                request.setResolutionSlaMet(now.isBefore(request.getResolutionSlaDeadline()));
            }
        }

        // When status moves to CLOSED, record closed timestamp
        if (newStatus == RequestStatus.CLOSED) {
            request.setClosedAt(now);
        }

        // If reopening (RESOLVED -> OPEN), reset SLA tracking
        if (newStatus == RequestStatus.OPEN && request.getResolvedAt() != null) {
            request.setResolvedAt(null);
            request.setResolutionSlaMet(null);
            request.setResponseSlaMet(null);
            request.setRespondedAt(null);
            // Note: SLA deadlines would need to be recalculated by the SLA service
            // This is handled in the main ServiceRequestService for now
        }
    }
}

