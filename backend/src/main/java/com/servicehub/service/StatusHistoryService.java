package com.servicehub.service;

import com.servicehub.dto.response.StatusHistoryResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.StatusHistory;
import com.servicehub.model.User;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing status history tracking and retrieval.
 * Handles recording status changes and transfers with proper audit trail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatusHistoryService {

    private final StatusHistoryRepository statusHistoryRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    /**
     * Records a standard status change transition.
     * Creates a status history entry for workflow transitions.
     *
     * @param request   the service request being updated
     * @param from      the previous status (can be null for initial creation)
     * @param to        the new status (required)
     * @param changedBy the user who triggered the status change
     * @param comment   optional comment describing the change
     * @return the saved StatusHistory entity
     */
    @Transactional
    public StatusHistory recordStatusChange(
            ServiceRequest request,
            RequestStatus from,
            RequestStatus to,
            User changedBy,
            String comment) {

        StatusHistory statusHistory = StatusHistory.builder()
                .request(request)
                .fromStatus(from != null ? from.name() : null)
                .toStatus(to.name())
                .changedBy(changedBy)
                .comment(comment)
                .build();

        StatusHistory saved = statusHistoryRepository.save(statusHistory);

        log.debug("Status change recorded: Request {} transitioned from {} to {} by {}",
                request.getReferenceNumber(),
                from != null ? from.name() : "null",
                to.name(),
                changedBy.getFullName());

        return saved;
    }

    /**
     * Records a ticket transfer between agents.
     * Ensures the comment (reason) is provided and saved.
     * Sets both fromAgent and toAgent for transfer tracking.
     *
     * @param request   the service request being transferred
     * @param fromAgent the agent who is transferring the ticket (can be null)
     * @param toAgent   the agent receiving the ticket (required)
     * @param changedBy the user who initiated the transfer
     * @param reason    the reason for the transfer (required)
     * @return the saved StatusHistory entity
     * @throws BadRequestException if reason is null or empty
     */
    @Transactional
    public StatusHistory recordTransfer(
            ServiceRequest request,
            User fromAgent,
            User toAgent,
            User changedBy,
            String reason) {

        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException("Transfer reason is required");
        }

        // Determine the status transition
        RequestStatus fromStatus = request.getStatus();
        RequestStatus toStatus = fromStatus == RequestStatus.IN_PROGRESS
                ? RequestStatus.ASSIGNED  // Revert to ASSIGNED if was IN_PROGRESS
                : RequestStatus.ASSIGNED; // Stay ASSIGNED if already ASSIGNED

        StatusHistory statusHistory = StatusHistory.builder()
                .request(request)
                .fromStatus(fromStatus.name())
                .toStatus(toStatus.name())
                .changedBy(changedBy)
                .fromAgent(fromAgent)
                .toAgent(toAgent)
                .comment(reason)
                .build();

        StatusHistory saved = statusHistoryRepository.save(statusHistory);

        log.info("Transfer recorded: Request {} transferred from {} to {} by {}",
                request.getReferenceNumber(),
                fromAgent != null ? fromAgent.getFullName() : "none",
                toAgent.getFullName(),
                changedBy.getFullName());

        return saved;
    }

    /**
     * Retrieves the complete status history timeline for a service request.
     * Returns records ordered by changed_at descending (most recent first).
     * Authorization: Owner, Assigned Agent, or ADMIN can access.
     *
     * @param requestId the ID of the service request
     * @param currentUserId the ID of the currently authenticated user
     * @param currentUserRole the role of the currently authenticated user
     * @return list of StatusHistoryResponse DTOs, most recent first
     * @throws ResourceNotFoundException if request not found
     * @throws org.springframework.security.access.AccessDeniedException if user is not authorized
     */
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getHistoryByRequestId(Long requestId, java.util.UUID currentUserId, com.servicehub.model.enums.Role currentUserRole) {
        // Fetch request and verify it exists
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service request not found with id: " + requestId));

        // Authorization check: Owner, Assigned Agent, or ADMIN can access
        boolean isOwner = request.getRequester() != null && request.getRequester().getId().equals(currentUserId);
        boolean isAssignedAgent = request.getAssignedAgent() != null && request.getAssignedAgent().getId().equals(currentUserId);
        boolean isAdmin = currentUserRole == com.servicehub.model.enums.Role.ADMIN;

        if (!isOwner && !isAssignedAgent && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: Only the request owner, assigned agent, or admin can view status history");
        }

        return statusHistoryRepository.findByRequestIdOrderByChangedAtDesc(requestId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Converts a StatusHistory entity to a StatusHistoryResponse DTO.
     *
     * @param history the StatusHistory entity
     * @return StatusHistoryResponse DTO
     */
    private StatusHistoryResponse toResponse(StatusHistory history) {
        return new StatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getFullName() : null,
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getFromAgent() != null ? history.getFromAgent().getFullName() : null,
                history.getToAgent() != null ? history.getToAgent().getFullName() : null,
                history.getComment(),
                history.getChangedAt()
        );
    }
}

