package com.servicehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestResponse {

    private Long id;
    private String referenceNumber;
    private String title;
    private String description;
    private String categoryName;
    private Long categoryId;
    private String priority;
    private String status;
    private String departmentName;
    private Long departmentId;
    private String locationName;
    private Long locationId;
    private String requesterName;
    private UUID requesterId;
    private String assignedAgentName;
    private UUID assignedAgentId;
    private LocalDateTime responseSlaDeadline;
    private LocalDateTime resolutionSlaDeadline;
    private Boolean responseSlaMet;
    private Boolean resolutionSlaMet;
    private LocalDateTime respondedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
