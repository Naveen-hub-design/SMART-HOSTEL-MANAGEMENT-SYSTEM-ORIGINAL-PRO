package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Attendance record details")
public class AttendanceDto {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "7")
    private Long studentId;

    @Schema(example = "Aditya Nair")
    private String studentName;

    @Schema(example = "ENR007")
    private String enrollmentNo;

    @Schema(example = "A-104")
    private String roomNo;

    @Schema(example = "A Wing - Senior Boys")
    private String blockName;

    @Schema(example = "2026-09-24")
    private LocalDate date;

    @Schema(example = "PRESENT")
    private String status;

    private String remarks;

    @Schema(description = "Display name of the user who marked the attendance")
    private String markedByName;

    private LocalDateTime markedAt;
}
