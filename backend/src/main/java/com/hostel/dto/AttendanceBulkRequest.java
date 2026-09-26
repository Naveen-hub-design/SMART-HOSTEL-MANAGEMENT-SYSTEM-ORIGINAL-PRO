package com.hostel.dto;

import com.hostel.entity.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Bulk attendance mark request for one date")
public class AttendanceBulkRequest {

    @NotNull(message = "Date is required")
    @Schema(example = "2026-09-24")
    private LocalDate date;

    @NotEmpty(message = "At least one attendance entry is required")
    @Valid
    @Builder.Default
    private List<AttendanceBulkEntry> entries = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Single row of a bulk attendance request")
    public static class AttendanceBulkEntry {

        @NotNull(message = "Student ID is required")
        @Schema(example = "7")
        private Long studentId;

        @NotNull(message = "Status is required")
        @Schema(example = "PRESENT")
        private AttendanceStatus status;

        @Schema(example = "Marked during morning muster")
        private String remarks;
    }
}
