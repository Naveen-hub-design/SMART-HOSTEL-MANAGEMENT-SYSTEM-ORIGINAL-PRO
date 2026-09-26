package com.hostel.dto;

import com.hostel.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Attendance correction request (status and remarks only; date is immutable)")
public class AttendanceCorrectionRequest {

    @NotNull(message = "Status is required")
    @Schema(example = "LATE")
    private AttendanceStatus status;

    @Schema(example = "Arrived late due to transport delay")
    private String remarks;
}
