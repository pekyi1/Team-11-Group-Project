package com.servicehub.controller;

import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.repository.UserRepository;
import com.servicehub.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/requests/{requestId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> uploadFile(@PathVariable Long requestId,
                                        @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.uploadFile(requestId, file, resolveCurrentUserId()));
    }

    @GetMapping
    public ResponseEntity<?> getAttachments(@PathVariable Long requestId) {
        return ResponseEntity.ok(attachmentService.getAttachments(requestId));
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long requestId,
                                                       @PathVariable Long attachmentId) {
        Resource resource = attachmentService.downloadAttachment(requestId, attachmentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long requestId,
                                                 @PathVariable Long attachmentId) {
        attachmentService.deleteAttachment(requestId, attachmentId, resolveCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    private UUID resolveCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email))
                .getId();
    }
}
