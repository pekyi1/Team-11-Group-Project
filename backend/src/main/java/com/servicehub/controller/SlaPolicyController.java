package com.servicehub.controller;

import com.servicehub.dto.request.SlaPolicyRequest;
import com.servicehub.dto.request.UpdateSlaPolicyRequest;
import com.servicehub.service.SlaPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for SLA policy management.
 * Provides CRUD operations for managing time-based SLA targets.
 */
@RestController
@RequestMapping("/api/v1/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    /**
     * Retrieves all SLA policies.
     * Accessible by any authenticated user.
     *
     * @return list of all SLA policies
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllPolicies() {
        return ResponseEntity.ok(slaPolicyService.getAllPolicies());
    }

    /**
     * Retrieves an SLA policy by ID.
     * Accessible by any authenticated user.
     *
     * @param id the policy ID
     * @return the SLA policy
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPolicyById(@PathVariable Integer id) {
        return ResponseEntity.ok(slaPolicyService.getPolicyById(id));
    }

    /**
     * Creates a new SLA policy.
     * Only accessible by ADMIN users.
     *
     * @param request the policy creation request
     * @return the created policy
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPolicy(@Valid @RequestBody SlaPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slaPolicyService.createPolicy(request));
    }

    /**
     * Updates an existing SLA policy.
     * Only accessible by ADMIN users.
     *
     * @param id the policy ID
     * @param request the update request
     * @return the updated policy
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePolicy(@PathVariable Integer id,
                                          @Valid @RequestBody UpdateSlaPolicyRequest request) {
        return ResponseEntity.ok(slaPolicyService.updatePolicy(id, request));
    }

    /**
     * Deletes an SLA policy.
     * Only accessible by ADMIN users.
     *
     * @param id the policy ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePolicy(@PathVariable Integer id) {
        slaPolicyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
