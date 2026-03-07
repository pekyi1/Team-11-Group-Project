package com.servicehub.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for status history records.
 * Uses Java Record for immutability and conciseness.
 * Formats data cleanly for frontend consumption with string representations
 * of statuses, user names, and timestamps.
 */
public record StatusHistoryResponse(
        Long id,
        String fromStatus,
        String toStatus,
        String changedByName,
        UUID changedById,
        String fromAgentName,
        String toAgentName,
        String comment,
        LocalDateTime changedAt
) {}
