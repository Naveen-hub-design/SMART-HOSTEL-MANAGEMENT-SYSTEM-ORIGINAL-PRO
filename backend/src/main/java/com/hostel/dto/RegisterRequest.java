package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Name is required")
    @Schema(example = "John Doe")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "john@hostel.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
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

    @Schema(hidden = true)
    private String role;

    @Schema(example = "Computer Science")
    private String department;

    @Schema(example = "M.Tech")
    private String qualification;

    /**
     * Room selected by the Warden while creating a student.
     * This is ignored for public registration.
     */
    @Schema(example = "12")
    private Long roomId;
}
