package com.servicehub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating an SLA policy.
 * Uses Java Record for immutability and conciseness.
 */
public record SlaPolicyRequest(
        @NotNull(message = "Category ID is required")
        Long categoryId,
        
        @NotBlank(message = "Priority is required")
        String priority,
        
        @NotNull(message = "Response time is required")
        @Min(value = 1, message = "Response time must be at least 1 hour")
        Integer responseTimeHours,
        
        @NotNull(message = "Resolution time is required")
        @Min(value = 1, message = "Resolution time must be at least 1 hour")
        Integer resolutionTimeHours
) {}
