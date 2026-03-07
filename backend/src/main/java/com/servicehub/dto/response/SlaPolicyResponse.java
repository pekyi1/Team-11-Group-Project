package com.servicehub.dto.response;

/**
 * Response DTO for SLA policy operations.
 * Uses Java Record for immutability and conciseness.
 */
public record SlaPolicyResponse(
        Integer id,
        Long categoryId,
        String categoryName,
        String priority,
        Integer responseTimeHours,
        Integer resolutionTimeHours,
        Boolean isActive
) {}
