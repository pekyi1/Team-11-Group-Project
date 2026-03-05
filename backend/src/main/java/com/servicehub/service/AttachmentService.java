package com.servicehub.service;

import com.servicehub.dto.response.AttachmentResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.Attachment;
import com.servicehub.model.ServiceRequest;
import com.servicehub.model.User;
import com.servicehub.repository.AttachmentRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.max-per-request}")
    private int maxPerRequest;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );

    @Transactional
    public AttachmentResponse uploadFile(Long requestId, MultipartFile file, UUID uploaderId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found with id: " + requestId));

        if (request.getIsDeleted()) {
            throw new BadRequestException("Cannot upload attachment to a deleted service request");
        }

        long existingCount = attachmentRepository.countByRequestIdAndIsDeletedFalse(requestId);
        if (existingCount >= maxPerRequest) {
            throw new BadRequestException("Maximum number of attachments (" + maxPerRequest + ") exceeded for this request");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("File type not allowed: " + contentType);
        }

        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + uploaderId));

        // Generate stored file name
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID() + extension;

        // Compute SHA-256 checksum
        String checksum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            checksum = sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new BadRequestException("Failed to compute file checksum: " + e.getMessage());
        }

        // Save file to disk
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("Failed to save file: " + e.getMessage());
        }

        Attachment attachment = Attachment.builder()
                .request(request)
                .fileName(originalFileName != null ? originalFileName : storedFileName)
                .storedFileName(storedFileName)
                .filePath(Paths.get(uploadDir, storedFileName).toString())
                .contentType(contentType)
                .fileSizeBytes(file.getSize())
                .checksum(checksum)
                .uploadedBy(uploader)
                .isDeleted(false)
                .build();

        attachment = attachmentRepository.save(attachment);
        log.info("Attachment uploaded: {} for request {}", originalFileName, requestId);
        return toResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachments(Long requestId) {
        return attachmentRepository.findByRequestIdAndIsDeletedFalse(requestId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource downloadAttachment(Long requestId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        if (!attachment.getRequest().getId().equals(requestId)) {
            throw new BadRequestException("Attachment does not belong to the specified request");
        }
        if (attachment.getIsDeleted()) {
            throw new ResourceNotFoundException("Attachment has been deleted");
        }

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found on disk: " + attachment.getStoredFileName());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + attachment.getStoredFileName());
        }
    }

    @Transactional
    public void deleteAttachment(Long requestId, Long attachmentId, UUID userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found with id: " + attachmentId));

        if (!attachment.getRequest().getId().equals(requestId)) {
            throw new BadRequestException("Attachment does not belong to the specified request");
        }

        attachment.setIsDeleted(true);
        attachmentRepository.save(attachment);
        log.info("Attachment {} soft-deleted by user {}", attachmentId, userId);
    }

    private AttachmentResponse toResponse(Attachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .requestId(a.getRequest().getId())
                .fileName(a.getFileName())
                .contentType(a.getContentType())
                .fileSizeBytes(a.getFileSizeBytes())
                .uploadedByName(a.getUploadedBy() != null ? a.getUploadedBy().getFullName() : null)
                .uploadedById(a.getUploadedBy() != null ? a.getUploadedBy().getId() : null)
                .createdAt(a.getCreatedAt())
                .build();
    }
}
