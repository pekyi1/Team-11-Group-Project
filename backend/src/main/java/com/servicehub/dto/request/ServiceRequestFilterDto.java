package com.servicehub.dto.request;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Data
@RequiredArgsConstructor
public class ServiceRequestFilterDto {
    private Long categoryId;
    private String priority;
    private String status;
    private Long locationId;
    private UUID requesterId;
    private UUID assignedAgentId;
}
