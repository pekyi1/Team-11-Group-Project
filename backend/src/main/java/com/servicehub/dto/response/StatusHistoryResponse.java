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
public class StatusHistoryResponse {

    private Long id;
    private String fromStatus;
    private String toStatus;
    private String changedByName;
    private UUID changedById;
    private String fromAgentName;
    private String toAgentName;
    private String comment;
    private LocalDateTime changedAt;
}
