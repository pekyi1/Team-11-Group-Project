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
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private String departmentName;
    private Long departmentId;
    private String locationName;
    private Long locationId;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
