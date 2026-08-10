package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login credentials")
public class LoginRequest {
    @Schema(example = "admin@hostel.com", description = "User email")
    private String email;

    @Schema(example = "password123", description = "User password")
    private String password;
}
