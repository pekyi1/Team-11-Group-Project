package com.servicehub.controller;

import com.servicehub.dto.*;
import com.servicehub.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class ServiceRequestController {
    private final ServiceRequestService requestService;

    @GetMapping
    public ResponseEntity<Page<ServiceRequestResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(requestService.getAllRequests(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    // TODO: POST create request with file attachments
    // TODO: PATCH /{id}/status — status transitions
    // TODO: POST /{id}/transfer — ticket transfer
    // TODO: PATCH /{id}/assign — admin assign agent
    // TODO: GET /{id}/history — status history
    // TODO: GET /{id}/attachments — list attachments
    // TODO: POST /{id}/attachments — upload files
}
