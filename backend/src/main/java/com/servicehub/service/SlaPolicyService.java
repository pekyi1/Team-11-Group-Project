package com.servicehub.service;

import com.servicehub.dto.request.SlaPolicyRequest;
import com.servicehub.dto.request.UpdateSlaPolicyRequest;
import com.servicehub.dto.response.SlaPolicyResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.exception.SlaPolicyNotFoundException;
import com.servicehub.model.Category;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.enums.Priority;
import com.servicehub.repository.CategoryRepository;
import com.servicehub.repository.SlaPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing SLA policies.
 * Handles CRUD operations and policy lookups for service request SLA calculations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Retrieves all SLA policies.
     *
     * @return list of all SLA policies
     */
    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllPolicies() {
        return slaPolicyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves an SLA policy by ID.
     *
     * @param id the policy ID
     * @return the SLA policy response
     * @throws SlaPolicyNotFoundException if policy not found
     */
    @Transactional(readOnly = true)
    public SlaPolicyResponse getPolicyById(Integer id) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new SlaPolicyNotFoundException("SLA policy not found with id: " + id));
        return toResponse(policy);
    }

    /**
     * Creates a new SLA policy.
     *
     * @param request the policy creation request
     * @return the created policy response
     * @throws DuplicateResourceException if a policy already exists for the category+priority combination
     * @throws BadRequestException if resolution time is less than response time
     */
    @Transactional
    public SlaPolicyResponse createPolicy(SlaPolicyRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        Priority priority = Priority.valueOf(request.priority().toUpperCase());

        // Check no existing active policy for same category + priority
        slaPolicyRepository.findByCategoryAndPriority(category, priority)
                .ifPresent(existing -> {
                    if (existing.getIsActive()) {
                        throw new DuplicateResourceException(
                                "Active SLA policy already exists for category " + category.getName()
                                        + " with priority " + priority);
                    }
                });

        if (request.resolutionTimeHours() < request.responseTimeHours()) {
            throw new BadRequestException("Resolution time must be greater than or equal to response time");
        }

        SlaPolicy policy = SlaPolicy.builder()
                .category(category)
                .priority(priority)
                .responseTimeHours(request.responseTimeHours())
                .resolutionTimeHours(request.resolutionTimeHours())
                .isActive(true)
                .build();

        policy = slaPolicyRepository.save(policy);
        log.info("SLA policy created for category {} with priority {}", category.getName(), priority);
        return toResponse(policy);
    }

    /**
     * Updates an existing SLA policy.
     *
     * @param id the policy ID
     * @param request the update request
     * @return the updated policy response
     * @throws SlaPolicyNotFoundException if policy not found
     * @throws BadRequestException if resolution time is less than response time
     */
    @Transactional
    public SlaPolicyResponse updatePolicy(Integer id, UpdateSlaPolicyRequest request) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new SlaPolicyNotFoundException("SLA policy not found with id: " + id));

        if (request.responseTimeHours() != null) {
            policy.setResponseTimeHours(request.responseTimeHours());
        }
        if (request.resolutionTimeHours() != null) {
            policy.setResolutionTimeHours(request.resolutionTimeHours());
        }
        if (request.isActive() != null) {
            policy.setIsActive(request.isActive());
        }

        // Validate resolution >= response if both are set
        if (policy.getResolutionTimeHours() < policy.getResponseTimeHours()) {
            throw new BadRequestException("Resolution time must be greater than or equal to response time");
        }

        policy = slaPolicyRepository.save(policy);
        log.info("SLA policy {} updated", id);
        return toResponse(policy);
    }

    /**
     * Deletes an SLA policy.
     *
     * @param id the policy ID
     * @throws SlaPolicyNotFoundException if policy not found
     */
    @Transactional
    public void deletePolicy(Integer id) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new SlaPolicyNotFoundException("SLA policy not found with id: " + id));
        slaPolicyRepository.delete(policy);
        log.info("SLA policy {} deleted", id);
    }

    /**
     * Looks up SLA policy target times for a given category and priority.
     * Used by service request creation to determine SLA deadlines.
     *
     * @param category the category entity
     * @param priority the priority enum value
     * @return the SLA policy with target times
     * @throws SlaPolicyNotFoundException if no active policy exists for the category+priority combination
     */
    @Transactional(readOnly = true)
    public SlaPolicy getTargetTimes(Category category, Priority priority) {
        return slaPolicyRepository.findByCategoryAndPriority(category, priority)
                .filter(SlaPolicy::getIsActive)
                .orElseThrow(() -> new SlaPolicyNotFoundException(
                        String.format("No active SLA policy found for category '%s' with priority '%s'",
                                category.getName(), priority)));
    }

    /**
     * Converts an SlaPolicy entity to a SlaPolicyResponse DTO.
     *
     * @param policy the entity
     * @return the response DTO
     */
    private SlaPolicyResponse toResponse(SlaPolicy policy) {
        return new SlaPolicyResponse(
                policy.getId(),
                policy.getCategory().getId(),
                policy.getCategory().getName(),
                policy.getPriority().name(),
                policy.getResponseTimeHours(),
                policy.getResolutionTimeHours(),
                policy.getIsActive()
        );
    }
}
