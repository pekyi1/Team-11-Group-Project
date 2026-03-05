package com.servicehub.service;

import com.servicehub.dto.request.CreateSlaPolicyRequest;
import com.servicehub.dto.request.UpdateSlaPolicyRequest;
import com.servicehub.dto.response.SlaPolicyResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> getAllPolicies() {
        return slaPolicyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SlaPolicyResponse createPolicy(CreateSlaPolicyRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        Priority priority = Priority.valueOf(request.getPriority().toUpperCase());

        // Check no existing active policy for same category + priority
        slaPolicyRepository.findByCategoryIdAndPriority(request.getCategoryId(), priority)
                .ifPresent(existing -> {
                    if (existing.getIsActive()) {
                        throw new DuplicateResourceException(
                                "Active SLA policy already exists for category " + category.getName()
                                        + " with priority " + priority);
                    }
                });

        if (request.getResolutionTimeMinutes() < request.getResponseTimeMinutes()) {
            throw new BadRequestException("Resolution time must be greater than or equal to response time");
        }

        SlaPolicy policy = SlaPolicy.builder()
                .category(category)
                .priority(priority)
                .responseTimeMinutes(request.getResponseTimeMinutes())
                .resolutionTimeMinutes(request.getResolutionTimeMinutes())
                .isActive(true)
                .build();

        policy = slaPolicyRepository.save(policy);
        log.info("SLA policy created for category {} with priority {}", category.getName(), priority);
        return toResponse(policy);
    }

    @Transactional
    public SlaPolicyResponse updatePolicy(Integer id, UpdateSlaPolicyRequest request) {
        SlaPolicy policy = slaPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA policy not found with id: " + id));

        if (request.getResponseTimeMinutes() != null) {
            policy.setResponseTimeMinutes(request.getResponseTimeMinutes());
        }
        if (request.getResolutionTimeMinutes() != null) {
            policy.setResolutionTimeMinutes(request.getResolutionTimeMinutes());
        }
        if (request.getIsActive() != null) {
            policy.setIsActive(request.getIsActive());
        }

        // Validate resolution >= response if both are set
        if (policy.getResolutionTimeMinutes() < policy.getResponseTimeMinutes()) {
            throw new BadRequestException("Resolution time must be greater than or equal to response time");
        }

        policy = slaPolicyRepository.save(policy);
        log.info("SLA policy {} updated", id);
        return toResponse(policy);
    }

    private SlaPolicyResponse toResponse(SlaPolicy p) {
        return SlaPolicyResponse.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .priority(p.getPriority().name())
                .responseTimeMinutes(p.getResponseTimeMinutes())
                .resolutionTimeMinutes(p.getResolutionTimeMinutes())
                .isActive(p.getIsActive())
                .build();
    }
}
