package com.servicehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaPolicyResponse {

    private Integer id;
    private Long categoryId;
    private String categoryName;
    private String priority;
    private Integer responseTimeMinutes;
    private Integer resolutionTimeMinutes;
    private Boolean isActive;
}
