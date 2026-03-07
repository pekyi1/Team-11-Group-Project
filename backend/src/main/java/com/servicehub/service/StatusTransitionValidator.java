package com.servicehub.service;

import com.servicehub.exception.InvalidStatusTransitionException;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates status transitions based on business rules.
 * Enforces role-based permissions for each transition.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StatusTransitionValidator {

    /**
     * Maps each status to the set of valid target statuses.
     */
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = Map.of(
            RequestStatus.OPEN, EnumSet.of(RequestStatus.ASSIGNED, RequestStatus.CLOSED),
            RequestStatus.ASSIGNED, EnumSet.of(RequestStatus.IN_PROGRESS, RequestStatus.ASSIGNED, RequestStatus.CLOSED),
            RequestStatus.IN_PROGRESS, EnumSet.of(RequestStatus.RESOLVED, RequestStatus.ASSIGNED, RequestStatus.CLOSED),
            RequestStatus.RESOLVED, EnumSet.of(RequestStatus.CLOSED, RequestStatus.OPEN),
            RequestStatus.CLOSED, EnumSet.noneOf(RequestStatus.class)
    );

    /**
     * Maps transitions to the roles allowed to perform them.
     * Key format: "FROM_STATUS->TO_STATUS"
     */
    private static final Map<String, Set<Role>> TRANSITION_ROLES = Map.ofEntries(
            // OPEN -> ASSIGNED: SYSTEM, ADMIN
            Map.entry("OPEN->ASSIGNED", EnumSet.of(Role.ADMIN)),
            // ASSIGNED -> IN_PROGRESS: AGENT
            Map.entry("ASSIGNED->IN_PROGRESS", EnumSet.of(Role.AGENT)),
            // IN_PROGRESS -> RESOLVED: AGENT
            Map.entry("IN_PROGRESS->RESOLVED", EnumSet.of(Role.AGENT)),
            // RESOLVED -> CLOSED: USER, ADMIN, SYSTEM
            Map.entry("RESOLVED->CLOSED", EnumSet.of(Role.USER, Role.ADMIN)),
            // RESOLVED -> OPEN: USER (reopen)
            Map.entry("RESOLVED->OPEN", EnumSet.of(Role.USER)),
            // ASSIGNED -> ASSIGNED: AGENT, ADMIN (transfer - handled separately)
            Map.entry("ASSIGNED->ASSIGNED", EnumSet.of(Role.AGENT, Role.ADMIN)),
            // IN_PROGRESS -> ASSIGNED: AGENT, ADMIN (transfer - handled separately)
            Map.entry("IN_PROGRESS->ASSIGNED", EnumSet.of(Role.AGENT, Role.ADMIN)),
            // Any (except CLOSED) -> CLOSED: ADMIN (force close)
            Map.entry("OPEN->CLOSED", EnumSet.of(Role.ADMIN)),
            Map.entry("ASSIGNED->CLOSED", EnumSet.of(Role.ADMIN)),
            Map.entry("IN_PROGRESS->CLOSED", EnumSet.of(Role.ADMIN))
    );

    /**
     * Validates a status transition for the given role.
     *
     * @param currentStatus the current status of the request
     * @param targetStatus  the desired target status
     * @param userRole      the role of the user attempting the transition
     * @throws InvalidStatusTransitionException if the transition is invalid
     */
    public void validateTransition(RequestStatus currentStatus, RequestStatus targetStatus, Role userRole) {
        // Check if transition is structurally valid
        Set<RequestStatus> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedTargets == null || !allowedTargets.contains(targetStatus)) {
            throw new InvalidStatusTransitionException(
                    String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
        }

        // Check if user role is allowed for this transition
        String transitionKey = currentStatus.name() + "->" + targetStatus.name();
        Set<Role> allowedRoles = TRANSITION_ROLES.get(transitionKey);

        if (allowedRoles == null || !allowedRoles.contains(userRole)) {
            String message = String.format(
                    "Role %s is not allowed to transition from %s to %s. Allowed roles: %s",
                    userRole, currentStatus, targetStatus, allowedRoles);
            log.warn("Invalid transition attempt: {}", message);
            throw new InvalidStatusTransitionException(message);
        }

        log.debug("Valid transition: {} -> {} by role {}", currentStatus, targetStatus, userRole);
    }

    /**
     * Checks if a transition is valid without throwing an exception.
     *
     * @param currentStatus the current status
     * @param targetStatus  the target status
     * @param userRole      the user role
     * @return true if valid, false otherwise
     */
    public boolean isValidTransition(RequestStatus currentStatus, RequestStatus targetStatus, Role userRole) {
        try {
            validateTransition(currentStatus, targetStatus, userRole);
            return true;
        } catch (InvalidStatusTransitionException e) {
            return false;
        }
    }
}

