package com.servicehub.repository;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {
    List<ServiceRequest> findByStatus(RequestStatus status);
    List<ServiceRequest> findByCategoryId(Long categoryId);
    List<ServiceRequest> findByAssignedAgentId(UUID agentId);
    List<ServiceRequest> findByRequesterId(UUID requesterId);
    Long countByStatus(RequestStatus status);
    List<ServiceRequest> findByResolutionSlaDeadlineBeforeAndStatusNotIn(LocalDateTime deadline, List<RequestStatus> statuses);
}
