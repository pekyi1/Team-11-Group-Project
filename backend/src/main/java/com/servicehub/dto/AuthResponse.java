package com.servicehub.dto;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private String fullName;
}
