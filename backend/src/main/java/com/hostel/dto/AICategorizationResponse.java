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
@Schema(description = "AI categorization response")
public class AICategorizationResponse {
    @Schema(example = "ELECTRICAL")
    private String category;

    @Schema(example = "0.95")
    private Double confidenceScore;

    @Schema(example = "[\"fan\", \"ceiling\", \"noise\"]")
    private List<String> matchedKeywords;
}
