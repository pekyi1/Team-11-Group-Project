package com.servicehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
    private Long categoryCount;
    private Long agentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
