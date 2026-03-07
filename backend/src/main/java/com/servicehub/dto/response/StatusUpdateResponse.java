package com.servicehub.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for status update operations.
 * Uses Java Record for immutability and conciseness.
 */
public record StatusUpdateResponse(
        Long requestId,
        String referenceNumber,
        String previousStatus,
        String newStatus,
        UUID changedById,
        String changedByName,
        String comment,
        LocalDateTime changedAt
) {}

