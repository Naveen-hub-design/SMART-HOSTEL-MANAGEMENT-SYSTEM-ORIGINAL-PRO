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
@Schema(description = "Authentication response with JWT")
public class AuthResponse {
    @Schema(example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;

    @Schema(example = "ADMIN")
    private String role;

    @Schema(example = "System Admin")
    private String name;

    @Schema(example = "admin@hostel.com")
    private String email;

    @Schema(example = "1")
    private Long userId;

    @Schema(example = "Login successful")
    private String message;
}
