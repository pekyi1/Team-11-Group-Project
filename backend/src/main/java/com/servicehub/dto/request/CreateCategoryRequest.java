package com.servicehub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Key is required")
    @Size(max = 50, message = "Key must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Key must start with an uppercase letter and contain only uppercase letters, digits, and underscores")
    private String key;

    private String description;

    @NotNull(message = "Department ID is required")
    private Long departmentId;
}
