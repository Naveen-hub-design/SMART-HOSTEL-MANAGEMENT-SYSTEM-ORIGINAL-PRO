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
@Schema(description = "A single recent activity shown on the admin dashboard")
public class RecentActivityDto {
    @Schema(example = "Warden created with email: warden@hostel.com")
    private String message;

    @Schema(example = "2026-08-05T10:30:00")
    private String date;
}
