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
@Schema(description = "Password change request")
public class PasswordChangeRequest {
    @Schema(example = "oldPassword123")
    private String currentPassword;

    @Schema(example = "newPassword456")
    private String newPassword;
}
