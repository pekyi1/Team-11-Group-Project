package com.servicehub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String address;

    private String city;

    private Boolean isActive;
}
