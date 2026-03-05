package com.servicehub.controller;

import com.servicehub.dto.request.AssignAgentRequest;
import com.servicehub.dto.request.CreateDepartmentRequest;
import com.servicehub.dto.request.UpdateDepartmentRequest;
import com.servicehub.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<?> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id,
                                              @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/agents")
    public ResponseEntity<?> getAgentsInDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getAgentsInDepartment(id));
    }

    @PostMapping("/{id}/agents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignAgentToDepartment(@PathVariable Long id,
                                                     @Valid @RequestBody AssignAgentRequest request) {
        return ResponseEntity.ok(departmentService.assignAgentToDepartment(id, request.getAgentId()));
    }

    @DeleteMapping("/{id}/agents/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeAgentFromDepartment(@PathVariable Long id,
                                                          @PathVariable UUID userId) {
        departmentService.removeAgentFromDepartment(id, userId);
        return ResponseEntity.noContent().build();
    }
}
