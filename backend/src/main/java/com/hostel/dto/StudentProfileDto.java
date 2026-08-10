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
@Schema(description = "Student profile details")
public class StudentProfileDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "John Doe")
    private String name;

    @Schema(example = "john@hostel.com")
    private String email;

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

    @Schema(example = "MALE")
    private String gender;

    @Schema(example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(example = "A101")
    private String roomNo;

    @Schema(example = "A-Block")
    private String blockName;

    @Schema(example = "1")
    private Long roomId;
}
