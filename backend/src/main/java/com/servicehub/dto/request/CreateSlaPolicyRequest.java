package com.servicehub.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSlaPolicyRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Priority is required")
    private String priority;

    @NotNull(message = "Response time is required")
    @Min(value = 1, message = "Response time must be at least 1 minute")
    private Integer responseTimeMinutes;

    @NotNull(message = "Resolution time is required")
    @Min(value = 1, message = "Resolution time must be at least 1 minute")
    private Integer resolutionTimeMinutes;
}
