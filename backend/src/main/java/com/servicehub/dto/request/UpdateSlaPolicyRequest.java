package com.servicehub.dto.request;

import jakarta.validation.constraints.Min;

/**
 * Request DTO for updating an SLA policy.
 * Uses Java Record for immutability and conciseness.
 * All fields are optional for partial updates.
 */
public record UpdateSlaPolicyRequest(
        @Min(value = 1, message = "Response time must be at least 1 hour")
        Integer responseTimeHours,
        
        @Min(value = 1, message = "Resolution time must be at least 1 hour")
        Integer resolutionTimeHours,
        
        Boolean isActive
) {}
