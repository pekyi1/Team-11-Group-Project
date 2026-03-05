package com.servicehub.service;

import com.servicehub.dto.request.AssignAgentRequest;
import com.servicehub.dto.request.CreateServiceRequestDto;
import com.servicehub.dto.request.StatusUpdateRequest;
import com.servicehub.dto.request.TransferRequest;
import com.servicehub.dto.response.ServiceRequestResponse;
import com.servicehub.dto.response.StatusHistoryResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.InvalidStatusTransitionException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.*;
import com.servicehub.model.enums.Priority;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.model.enums.Role;
import com.servicehub.event.ServiceRequestEvent;
import com.servicehub.repository.*;
import com.servicehub.repository.specification.ServiceRequestSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final LocationRepository locationRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(RequestStatus.class);
        ALLOWED_TRANSITIONS.put(RequestStatus.OPEN, EnumSet.of(RequestStatus.ASSIGNED, RequestStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(RequestStatus.ASSIGNED, EnumSet.of(RequestStatus.IN_PROGRESS, RequestStatus.OPEN));
        ALLOWED_TRANSITIONS.put(RequestStatus.IN_PROGRESS, EnumSet.of(RequestStatus.RESOLVED));
        ALLOWED_TRANSITIONS.put(RequestStatus.RESOLVED, EnumSet.of(RequestStatus.CLOSED, RequestStatus.OPEN));
        ALLOWED_TRANSITIONS.put(RequestStatus.CLOSED, EnumSet.noneOf(RequestStatus.class));
    }

    @Transactional
    public ServiceRequestResponse createRequest(CreateServiceRequestDto dto, UUID requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requesterId));

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + dto.getCategoryId()));

        if (!category.getIsActive()) {
            throw new BadRequestException("Category is not active: " + category.getName());
        }

        Department department = category.getDepartment();
        Priority priority = Priority.valueOf(dto.getPriority().toUpperCase());
        String referenceNumber = generateReferenceNumber();

        ServiceRequest request = ServiceRequest.builder()
                .referenceNumber(referenceNumber)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .priority(priority)
                .status(RequestStatus.OPEN)
                .department(department)
                .location(requester.getLocation())
                .requester(requester)
                .isDeleted(false)
                .build();

        // Look up SLA policy and compute deadlines
        slaPolicyRepository.findByCategoryIdAndPriority(category.getId(), priority)
                .ifPresent(sla -> {
                    LocalDateTime now = LocalDateTime.now();
                    request.setResponseSlaDeadline(now.plusMinutes(sla.getResponseTimeMinutes()));
                    request.setResolutionSlaDeadline(now.plusMinutes(sla.getResolutionTimeMinutes()));
                });

        ServiceRequest savedRequest = serviceRequestRepository.save(request);

        // Create initial status history entry
        StatusHistory initialHistory = StatusHistory.builder()
                .request(savedRequest)
                .fromStatus(null)
                .toStatus(RequestStatus.OPEN.name())
                .changedBy(requester)
                .comment("Service request created")
                .build();
        statusHistoryRepository.save(initialHistory);

        // Auto-route: try to find an agent in the same department
        autoAssignAgent(savedRequest, requester);

        log.info("Service request created: {}", referenceNumber);

        // Publish REQUEST_CREATED event
        eventPublisher.publishEvent(new ServiceRequestEvent(this, savedRequest, "REQUEST_CREATED", requester, "Service request created"));
        // If auto-assigned, also publish REQUEST_ASSIGNED
        if (savedRequest.getAssignedAgent() != null) {
            eventPublisher.publishEvent(new ServiceRequestEvent(this, savedRequest, "REQUEST_ASSIGNED", requester, "Auto-assigned to agent"));
        }

        return toResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse getRequestById(Long id) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + id));
        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> getAllRequests(Pageable pageable, Long categoryId,
            String priority, String status, Long locationId, UUID requesterId, UUID assignedAgentId) {
        Specification<ServiceRequest> spec = ServiceRequestSpecification.notDeleted();

        if (categoryId != null) spec = spec.and(ServiceRequestSpecification.hasCategoryId(categoryId));
        if (priority != null) spec = spec.and(ServiceRequestSpecification.hasPriority(priority));
        if (status != null) spec = spec.and(ServiceRequestSpecification.hasStatus(status));
        if (locationId != null) spec = spec.and(ServiceRequestSpecification.hasLocationId(locationId));
        if (requesterId != null) spec = spec.and(ServiceRequestSpecification.hasRequesterId(requesterId));
        if (assignedAgentId != null) spec = spec.and(ServiceRequestSpecification.hasAssignedAgentId(assignedAgentId));

        return serviceRequestRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(Long id, StatusUpdateRequest request, UUID actorId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + id));

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + actorId));

        RequestStatus currentStatus = serviceRequest.getStatus();
        RequestStatus newStatus = RequestStatus.valueOf(request.getNewStatus().toUpperCase());

        // Validate status transition
        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(RequestStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        String fromStatus = currentStatus.name();
        serviceRequest.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        if (newStatus == RequestStatus.IN_PROGRESS) {
            serviceRequest.setRespondedAt(now);
            if (serviceRequest.getResponseSlaDeadline() != null) {
                serviceRequest.setResponseSlaMet(now.isBefore(serviceRequest.getResponseSlaDeadline()));
            }
        }

        if (newStatus == RequestStatus.RESOLVED) {
            serviceRequest.setResolvedAt(now);
            if (serviceRequest.getResolutionSlaDeadline() != null) {
                serviceRequest.setResolutionSlaMet(now.isBefore(serviceRequest.getResolutionSlaDeadline()));
            }
        }

        if (newStatus == RequestStatus.CLOSED) {
            serviceRequest.setClosedAt(now);
        }

        // If reopening (RESOLVED -> OPEN), recalculate SLA deadlines
        if (currentStatus == RequestStatus.RESOLVED && newStatus == RequestStatus.OPEN) {
            serviceRequest.setResolvedAt(null);
            serviceRequest.setResolutionSlaMet(null);
            serviceRequest.setResponseSlaMet(null);
            serviceRequest.setRespondedAt(null);
            slaPolicyRepository.findByCategoryIdAndPriority(
                    serviceRequest.getCategory().getId(), serviceRequest.getPriority())
                    .ifPresent(sla -> {
                        serviceRequest.setResponseSlaDeadline(now.plusMinutes(sla.getResponseTimeMinutes()));
                        serviceRequest.setResolutionSlaDeadline(now.plusMinutes(sla.getResolutionTimeMinutes()));
                    });
        }

        // Create status history entry
        StatusHistory history = StatusHistory.builder()
                .request(serviceRequest)
                .fromStatus(fromStatus)
                .toStatus(newStatus.name())
                .changedBy(actor)
                .comment(request.getComment())
                .build();
        statusHistoryRepository.save(history);

        ServiceRequest saved = serviceRequestRepository.save(serviceRequest);
        log.info("Service request {} status updated: {} -> {}", saved.getReferenceNumber(), fromStatus, newStatus);

        // Publish status change events
        String eventType = switch (newStatus) {
            case IN_PROGRESS -> "STATUS_IN_PROGRESS";
            case RESOLVED -> "STATUS_RESOLVED";
            case CLOSED -> "STATUS_CLOSED";
            default -> null;
        };
        if (currentStatus == RequestStatus.RESOLVED && newStatus == RequestStatus.OPEN) {
            eventType = "REQUEST_REOPENED";
        }
        if (eventType != null) {
            eventPublisher.publishEvent(new ServiceRequestEvent(this, saved, eventType, actor, request.getComment()));
        }

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse transferRequest(Long id, TransferRequest request, UUID actorId) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + id));

        if (serviceRequest.getStatus() != RequestStatus.ASSIGNED
                && serviceRequest.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new BadRequestException("Request can only be transferred when status is ASSIGNED or IN_PROGRESS");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + actorId));

        User targetAgent = userRepository.findById(request.getTargetAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Target agent not found with id: " + request.getTargetAgentId()));

        if (targetAgent.getRole() != Role.AGENT) {
            throw new BadRequestException("Target user is not an agent");
        }
        if (!targetAgent.getIsActive()) {
            throw new BadRequestException("Target agent is not active");
        }
        if (targetAgent.getDepartment() == null
                || !targetAgent.getDepartment().getId().equals(serviceRequest.getDepartment().getId())) {
            throw new BadRequestException("Target agent must belong to the same department");
        }

        User previousAgent = serviceRequest.getAssignedAgent();
        serviceRequest.setAssignedAgent(targetAgent);

        // If status was IN_PROGRESS, revert to ASSIGNED
        String fromStatus = serviceRequest.getStatus().name();
        if (serviceRequest.getStatus() == RequestStatus.IN_PROGRESS) {
            serviceRequest.setStatus(RequestStatus.ASSIGNED);
        }

        // Create status history with transfer details
        StatusHistory history = StatusHistory.builder()
                .request(serviceRequest)
                .fromStatus(fromStatus)
                .toStatus(serviceRequest.getStatus().name())
                .changedBy(actor)
                .fromAgent(previousAgent)
                .toAgent(targetAgent)
                .comment(request.getReason())
                .build();
        statusHistoryRepository.save(history);

        serviceRequest = serviceRequestRepository.save(serviceRequest);
        log.info("Service request {} transferred from {} to {}",
                serviceRequest.getReferenceNumber(),
                previousAgent != null ? previousAgent.getFullName() : "none",
                targetAgent.getFullName());

        // Publish TICKET_TRANSFERRED event
        eventPublisher.publishEvent(new ServiceRequestEvent(this, serviceRequest, "TICKET_TRANSFERRED", actor, request.getReason(), previousAgent));

        return toResponse(serviceRequest);
    }

    @Transactional
    public ServiceRequestResponse assignRequest(Long id, AssignAgentRequest request) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + id));

        User agent = userRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with id: " + request.getAgentId()));

        if (agent.getRole() != Role.AGENT) {
            throw new BadRequestException("User is not an agent");
        }
        if (!agent.getIsActive()) {
            throw new BadRequestException("Agent is not active");
        }

        String fromStatus = serviceRequest.getStatus().name();
        serviceRequest.setAssignedAgent(agent);

        if (serviceRequest.getStatus() == RequestStatus.OPEN) {
            serviceRequest.setStatus(RequestStatus.ASSIGNED);
        }

        // Create status history entry
        StatusHistory history = StatusHistory.builder()
                .request(serviceRequest)
                .fromStatus(fromStatus)
                .toStatus(serviceRequest.getStatus().name())
                .changedBy(agent)
                .toAgent(agent)
                .comment("Agent assigned by admin")
                .build();
        statusHistoryRepository.save(history);

        serviceRequest = serviceRequestRepository.save(serviceRequest);
        log.info("Service request {} assigned to {}", serviceRequest.getReferenceNumber(), agent.getFullName());

        // Publish REQUEST_ASSIGNED event
        eventPublisher.publishEvent(new ServiceRequestEvent(this, serviceRequest, "REQUEST_ASSIGNED", agent, "Agent assigned by admin"));

        return toResponse(serviceRequest);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getRequestHistory(Long id) {
        serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + id));

        return statusHistoryRepository.findByRequestIdOrderByChangedAtAsc(id).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    private void autoAssignAgent(ServiceRequest request, User requester) {
        Department department = request.getDepartment();
        if (department == null) return;

        // Find all active agents in the same department
        List<User> departmentAgents = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.AGENT
                        && u.getIsActive()
                        && u.getDepartment() != null
                        && u.getDepartment().getId().equals(department.getId()))
                .collect(Collectors.toList());

        if (departmentAgents.isEmpty()) return;

        // Try same location first
        Location requesterLocation = requester.getLocation();
        Optional<User> sameLocationAgent = departmentAgents.stream()
                .filter(a -> a.getLocation() != null
                        && a.getLocation().getId().equals(requesterLocation.getId()))
                .min(Comparator.comparingLong(a -> countActiveTickets(a.getId())));

        User selectedAgent;
        if (sameLocationAgent.isPresent()) {
            selectedAgent = sameLocationAgent.get();
        } else {
            // Fall back to any agent in the department with least active tickets
            selectedAgent = departmentAgents.stream()
                    .min(Comparator.comparingLong(a -> countActiveTickets(a.getId())))
                    .orElse(null);
        }

        if (selectedAgent != null) {
            request.setAssignedAgent(selectedAgent);
            request.setStatus(RequestStatus.ASSIGNED);
            serviceRequestRepository.save(request);

            StatusHistory assignHistory = StatusHistory.builder()
                    .request(request)
                    .fromStatus(RequestStatus.OPEN.name())
                    .toStatus(RequestStatus.ASSIGNED.name())
                    .changedBy(requester)
                    .toAgent(selectedAgent)
                    .comment("Auto-assigned to agent")
                    .build();
            statusHistoryRepository.save(assignHistory);

            log.info("Service request {} auto-assigned to {}", request.getReferenceNumber(), selectedAgent.getFullName());
        }
    }

    private long countActiveTickets(UUID agentId) {
        List<ServiceRequest> agentRequests = serviceRequestRepository.findByAssignedAgentId(agentId);
        return agentRequests.stream()
                .filter(r -> r.getStatus() == RequestStatus.ASSIGNED || r.getStatus() == RequestStatus.IN_PROGRESS)
                .count();
    }

    private String generateReferenceNumber() {
        int year = Year.now().getValue();
        long count = serviceRequestRepository.count();
        return String.format("SR-%d-%05d", year, count + 1);
    }

    private ServiceRequestResponse toResponse(ServiceRequest r) {
        return ServiceRequestResponse.builder()
                .id(r.getId())
                .referenceNumber(r.getReferenceNumber())
                .title(r.getTitle())
                .description(r.getDescription())
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .priority(r.getPriority() != null ? r.getPriority().name() : null)
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .departmentName(r.getDepartment() != null ? r.getDepartment().getName() : null)
                .departmentId(r.getDepartment() != null ? r.getDepartment().getId() : null)
                .locationName(r.getLocation() != null ? r.getLocation().getName() : null)
                .locationId(r.getLocation() != null ? r.getLocation().getId() : null)
                .requesterName(r.getRequester() != null ? r.getRequester().getFullName() : null)
                .requesterId(r.getRequester() != null ? r.getRequester().getId() : null)
                .assignedAgentName(r.getAssignedAgent() != null ? r.getAssignedAgent().getFullName() : null)
                .assignedAgentId(r.getAssignedAgent() != null ? r.getAssignedAgent().getId() : null)
                .responseSlaDeadline(r.getResponseSlaDeadline())
                .resolutionSlaDeadline(r.getResolutionSlaDeadline())
                .responseSlaMet(r.getResponseSlaMet())
                .resolutionSlaMet(r.getResolutionSlaMet())
                .respondedAt(r.getRespondedAt())
                .resolvedAt(r.getResolvedAt())
                .closedAt(r.getClosedAt())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private StatusHistoryResponse toHistoryResponse(StatusHistory h) {
        return StatusHistoryResponse.builder()
                .id(h.getId())
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : null)
                .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .fromAgentName(h.getFromAgent() != null ? h.getFromAgent().getFullName() : null)
                .toAgentName(h.getToAgent() != null ? h.getToAgent().getFullName() : null)
                .comment(h.getComment())
                .changedAt(h.getChangedAt())
                .build();
    }
}
