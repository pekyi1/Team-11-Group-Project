package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.model.*;
import com.servicehub.model.enums.*;
import com.servicehub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {
    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public Page<ServiceRequestResponse> getAllRequests(int page, int size) {
        return requestRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    public ServiceRequestResponse getRequestById(Long id) {
        return toResponse(requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found")));
    }

    // TODO: Implement createRequest with new Category entity lookup
    // TODO: Implement updateStatus with status workflow validation
    // TODO: Implement assignRequest(Long requestId, UUID agentId)
    // TODO: Implement transferRequest(Long requestId, UUID targetAgentId, String reason)
    // TODO: Implement getRequestsByRequester(UUID userId)
    // TODO: Implement getDashboardStats()
    // TODO: Implement SLA breach detection

    private ServiceRequestResponse toResponse(ServiceRequest req) {
        return ServiceRequestResponse.builder()
                .id(req.getId())
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory().getName() : null)
                .priority(req.getPriority().name())
                .status(req.getStatus().name())
                .requesterName(req.getRequester().getFullName())
                .assignedToName(req.getAssignedAgent() != null ? req.getAssignedAgent().getFullName() : null)
                .departmentName(req.getDepartment() != null ? req.getDepartment().getName() : null)
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .resolvedAt(req.getResolvedAt())
                .build();
    }
}
