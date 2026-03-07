package com.servicehub.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating service request status.
 * Uses Java Record for immutability and conciseness.
 */
public record StatusUpdateRequest(
        @NotBlank(message = "New status is required")
        String newStatus,
        String comment
) {}
