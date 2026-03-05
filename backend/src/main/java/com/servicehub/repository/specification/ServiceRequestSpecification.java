package com.servicehub.repository.specification;

import com.servicehub.model.ServiceRequest;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ServiceRequestSpecification {

    private ServiceRequestSpecification() {}

    public static Specification<ServiceRequest> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<ServiceRequest> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<ServiceRequest> hasPriority(String priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), Priority.valueOf(priority.toUpperCase()));
    }

    public static Specification<ServiceRequest> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), RequestStatus.valueOf(status.toUpperCase()));
    }

    public static Specification<ServiceRequest> hasLocationId(Long locationId) {
        return (root, query, cb) -> cb.equal(root.get("location").get("id"), locationId);
    }

    public static Specification<ServiceRequest> hasRequesterId(UUID requesterId) {
        return (root, query, cb) -> cb.equal(root.get("requester").get("id"), requesterId);
    }

    public static Specification<ServiceRequest> hasAssignedAgentId(UUID agentId) {
        return (root, query, cb) -> cb.equal(root.get("assignedAgent").get("id"), agentId);
    }
}
