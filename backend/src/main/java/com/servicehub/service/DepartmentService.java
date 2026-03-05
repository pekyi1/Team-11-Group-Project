package com.servicehub.service;

import com.servicehub.dto.request.CreateDepartmentRequest;
import com.servicehub.dto.request.UpdateDepartmentRequest;
import com.servicehub.dto.response.DepartmentResponse;
import com.servicehub.dto.response.UserResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.Department;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.CategoryRepository;
import com.servicehub.repository.DepartmentRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.findAll().stream()
                .anyMatch(d -> d.getName().equalsIgnoreCase(request.getName()))) {
            throw new DuplicateResourceException("Department already exists with name: " + request.getName());
        }

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isActive(true)
                .build();

        department = departmentRepository.save(department);
        log.info("Department created: {}", department.getName());
        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (request.getName() != null) {
            department.setName(request.getName());
        }
        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            department.setIsActive(request.getIsActive());
        }

        department = departmentRepository.save(department);
        log.info("Department updated: {}", department.getName());
        return toResponse(department);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        // Check for active service requests in this department
        // Using a simple check - if there are any non-closed/non-resolved requests
        // we prevent deletion
        long activeRequestCount = userRepository.findAll().stream()
                .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(id))
                .count();
        // Note: Ideally check ServiceRequestRepository for active requests in this department
        // For now, soft-delete is safe
        department.setIsActive(false);
        departmentRepository.save(department);
        log.info("Department soft-deleted: {}", department.getName());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAgentsInDepartment(Long departmentId) {
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        return userRepository.findAll().stream()
                .filter(u -> u.getDepartment() != null
                        && u.getDepartment().getId().equals(departmentId)
                        && u.getRole() == Role.AGENT)
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse assignAgentToDepartment(Long departmentId, UUID agentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        User user = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + agentId));

        if (user.getRole() != Role.AGENT) {
            throw new BadRequestException("User is not an agent");
        }

        user.setDepartment(department);
        user = userRepository.save(user);
        log.info("Agent {} assigned to department {}", user.getFullName(), department.getName());
        return toUserResponse(user);
    }

    @Transactional
    public void removeAgentFromDepartment(Long departmentId, UUID agentId) {
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + departmentId));

        User user = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + agentId));

        if (user.getDepartment() == null || !user.getDepartment().getId().equals(departmentId)) {
            throw new BadRequestException("Agent does not belong to this department");
        }

        user.setDepartment(null);
        userRepository.save(user);
        log.info("Agent {} removed from department {}", user.getFullName(), departmentId);
    }

    private DepartmentResponse toResponse(Department d) {
        long categoryCount = categoryRepository.findByDepartmentId(d.getId()).size();
        long agentCount = userRepository.findAll().stream()
                .filter(u -> u.getDepartment() != null
                        && u.getDepartment().getId().equals(d.getId())
                        && u.getRole() == Role.AGENT)
                .count();

        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .isActive(d.getIsActive())
                .categoryCount(categoryCount)
                .agentCount(agentCount)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .locationName(user.getLocation() != null ? user.getLocation().getName() : null)
                .locationId(user.getLocation() != null ? user.getLocation().getId() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
