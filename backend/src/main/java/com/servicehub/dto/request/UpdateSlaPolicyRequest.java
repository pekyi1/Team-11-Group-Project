package com.servicehub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSlaPolicyRequest {

    private Integer responseTimeMinutes;

    private Integer resolutionTimeMinutes;

    private Boolean isActive;
}
