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
@Schema(description = "Safe account profile for the authenticated warden or admin. Never carries credentials.")
public class AccountProfileDto {

    @Schema(example = "3")
    private Long id;

    @Schema(example = "Jane Warden")
    private String name;

    @Schema(example = "warden@hostel.com")
    private String email;

    @Schema(example = "9876543211")
    private String phone;

    @Schema(example = "WARDEN")
    private String role;

    @Schema(description = "Warden qualification, if applicable")
    private String qualification;

    @Schema(description = "Admin department, if applicable")
    private String department;

    @Schema(description = "Assigned hostel block display name (wardens only)")
    private String blockName;

    @Schema(description = "Profile image URL served from /uploads")
    private String profileImageUrl;
}
