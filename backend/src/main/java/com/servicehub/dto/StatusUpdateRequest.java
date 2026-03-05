package com.servicehub.dto;

import lombok.*;

@Data
public class StatusUpdateRequest {
    private String newStatus;
    private String comment;
}
