package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Result of bulk student import")
public class BulkImportResultDto {

    @Schema(example = "100")
    private int totalRows;

    @Schema(example = "94")
    private int successCount;

    @Schema(example = "6")
    private int failureCount;

    @Schema(description = "Per-row import results")
    private List<RowResult> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Result of a single imported row")
    public static class RowResult {

        @Schema(example = "2")
        private int rowNumber;

        @Schema(example = "John Doe")
        private String name;

        @Schema(example = "john@example.com")
        private String email;

        @Schema(example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
        private String status;

        @Schema(example = "A-101")
        private String room;

        @Schema(example = "Student created successfully")
        private String message;
    }
}
