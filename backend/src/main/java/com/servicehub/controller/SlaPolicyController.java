package com.servicehub.controller;

import com.servicehub.dto.request.CreateSlaPolicyRequest;
import com.servicehub.dto.request.UpdateSlaPolicyRequest;
import com.servicehub.service.SlaPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/sla-policies")
@RequiredArgsConstructor
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPolicies() {
        return ResponseEntity.ok(slaPolicyService.getAllPolicies());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPolicy(@Valid @RequestBody CreateSlaPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slaPolicyService.createPolicy(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePolicy(@PathVariable Integer id,
                                          @Valid @RequestBody UpdateSlaPolicyRequest request) {
        return ResponseEntity.ok(slaPolicyService.updatePolicy(id, request));
    }
}
