package com.servicehub.controller;

import com.servicehub.dto.request.AssignAgentRequest;
import com.servicehub.dto.request.CreateServiceRequestDto;
import com.servicehub.dto.request.StatusUpdateRequest;
import com.servicehub.dto.request.TransferRequest;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.enums.RequestStatus;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.RequestWorkflowService;
import com.servicehub.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final RequestWorkflowService requestWorkflowService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getAllRequests(Pageable pageable,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String priority,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long locationId,
                                           @RequestParam(required = false) UUID requesterId,
                                           @RequestParam(required = false) UUID assignedAgentId) {
        return ResponseEntity.ok(serviceRequestService.getAllRequests(
                pageable, categoryId, priority, status, locationId, requesterId, assignedAgentId));
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@Valid @RequestBody CreateServiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceRequestService.createRequest(dto, resolveCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getRequestById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody StatusUpdateRequest request) {
        RequestStatus newStatus = RequestStatus.valueOf(request.newStatus().toUpperCase());
        return ResponseEntity.ok(requestWorkflowService.updateStatus(id, newStatus, request.comment()));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transferRequest(@PathVariable Long id,
                                             @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(serviceRequestService.transferRequest(id, request, resolveCurrentUserId()));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRequest(@PathVariable Long id,
                                           @Valid @RequestBody AssignAgentRequest request) {
        return ResponseEntity.ok(serviceRequestService.assignRequest(id, request));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getRequestHistory(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getRequestHistory(id));
    }

    private UUID resolveCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email))
                .getId();
    }
}
