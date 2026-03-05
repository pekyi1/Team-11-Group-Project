package com.servicehub.service;

import com.servicehub.dto.response.DashboardSummaryResponse;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ServiceRequestRepository serviceRequestRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long openCount = serviceRequestRepository.countByStatus(RequestStatus.OPEN);
        long assignedCount = serviceRequestRepository.countByStatus(RequestStatus.ASSIGNED);
        long inProgressCount = serviceRequestRepository.countByStatus(RequestStatus.IN_PROGRESS);
        long resolvedCount = serviceRequestRepository.countByStatus(RequestStatus.RESOLVED);
        long closedCount = serviceRequestRepository.countByStatus(RequestStatus.CLOSED);
        long total = openCount + assignedCount + inProgressCount + resolvedCount + closedCount;

        // SLA compliance rate: count where resolutionSlaMet=true / count where resolutionSlaMet is not null
        List<ServiceRequest> allRequests = serviceRequestRepository.findAll();
        long slaEvaluated = allRequests.stream()
                .filter(r -> r.getResolutionSlaMet() != null)
                .count();
        long slaMet = allRequests.stream()
                .filter(r -> Boolean.TRUE.equals(r.getResolutionSlaMet()))
                .count();
        double slaComplianceRate = slaEvaluated > 0 ? (double) slaMet / slaEvaluated * 100.0 : 0.0;

        // TODO: Compute average resolution hours from resolvedAt - createdAt for resolved/closed requests
        double avgResolutionHours = 0.0;

        return DashboardSummaryResponse.builder()
                .totalRequests(total)
                .openRequests(openCount)
                .assignedRequests(assignedCount)
                .inProgressRequests(inProgressCount)
                .resolvedRequests(resolvedCount)
                .closedRequests(closedCount)
                .slaComplianceRate(slaComplianceRate)
                .avgResolutionHours(avgResolutionHours)
                .build();
    }
}
