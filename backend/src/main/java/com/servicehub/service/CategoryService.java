package com.servicehub.service;

import com.servicehub.dto.request.CreateCategoryRequest;
import com.servicehub.dto.request.UpdateCategoryRequest;
import com.servicehub.dto.response.CategoryResponse;
import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.Category;
import com.servicehub.model.Department;
import com.servicehub.repository.CategoryRepository;
import com.servicehub.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getCategoriesByDepartment(Long departmentId) {
        return categoryRepository.findByDepartmentIdAndIsActiveTrue(departmentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.getName());
        }
        if (categoryRepository.existsByKey(request.getKey())) {
            throw new DuplicateResourceException("Category already exists with key: " + request.getKey());
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        Category category = Category.builder()
                .name(request.getName())
                .key(request.getKey())
                .description(request.getDescription())
                .department(department)
                .isActive(true)
                .build();

        category = categoryRepository.save(category);
        log.info("Category created: {}", category.getName());
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));
            category.setDepartment(department);
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        category = categoryRepository.save(category);
        log.info("Category updated: {}", category.getName());
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        category.setIsActive(false);
        category = categoryRepository.save(category);
        log.info("Category deactivated: {}", category.getName());
        return toResponse(category);
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .key(c.getKey())
                .description(c.getDescription())
                .departmentId(c.getDepartment().getId())
                .departmentName(c.getDepartment().getName())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
