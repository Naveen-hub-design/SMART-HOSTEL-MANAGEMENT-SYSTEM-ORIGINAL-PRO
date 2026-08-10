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
@Schema(description = "Registration request")
public class RegisterRequest {
    @Schema(example = "John Doe")
    private String name;

    @Schema(example = "john@hostel.com")
    private String email;

    @Schema(example = "password123")
    private String password;

    @Schema(example = "9876543210")
    private String phone;

    @Schema(example = "ENR2024001")
    private String enrollmentNo;

    @Schema(example = "9876543211")
    private String parentContact;

    @Schema(example = "123 Main Street")
    private String address;

    @Schema(example = "2000-01-15")
    private String dateOfBirth;

    @Schema(example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    private String gender;

    @Schema(example = "STUDENT", allowableValues = {"STUDENT", "WARDEN", "ADMIN"})
    private String role;

    @Schema(example = "Computer Science")
    private String department;

    @Schema(example = "M.Tech")
    private String qualification;
}
