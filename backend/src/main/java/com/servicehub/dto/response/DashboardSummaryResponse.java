package com.servicehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private Long totalRequests;
    private Long openRequests;
    private Long assignedRequests;
    private Long inProgressRequests;
    private Long resolvedRequests;
    private Long closedRequests;
    private Double slaComplianceRate;
    private Double avgResolutionHours;
}
