package com.hostel.dto;

import com.hostel.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Single attendance mark request")
public class AttendanceMarkRequest {

    @NotNull(message = "Student ID is required")
    @Schema(example = "7")
    private Long studentId;

    @NotNull(message = "Date is required")
    @Schema(example = "2026-09-24")
    private LocalDate date;

    @NotNull(message = "Status is required")
    @Schema(example = "PRESENT")
    private AttendanceStatus status;

    @Schema(example = "Marked during morning muster")
    private String remarks;
}
