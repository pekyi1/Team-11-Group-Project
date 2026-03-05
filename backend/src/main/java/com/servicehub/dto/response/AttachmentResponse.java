package com.servicehub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private Long id;
    private Long requestId;
    private String fileName;
    private String contentType;
    private Long fileSizeBytes;
    private String uploadedByName;
    private UUID uploadedById;
    private LocalDateTime createdAt;
}
