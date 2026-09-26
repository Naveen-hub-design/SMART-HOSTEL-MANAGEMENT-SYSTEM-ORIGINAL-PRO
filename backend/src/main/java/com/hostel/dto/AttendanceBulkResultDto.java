package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Per-row result of a bulk attendance operation")
public class AttendanceBulkResultDto {

    @Schema(example = "20")
    private int totalRows;

    @Schema(example = "18")
    private int successCount;

    @Schema(example = "2")
    private int failureCount;

    @Builder.Default
    private List<RowResult> results = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Result of a single bulk attendance row")
    public static class RowResult {

        @Schema(example = "1")
        private int rowNumber;

        @Schema(example = "7")
        private Long studentId;

        @Schema(example = "Aditya Nair")
        private String studentName;

        @Schema(example = "SUCCESS")
        private String status;

        @Schema(example = "A-104")
        private String room;

        private String message;
    }
}
