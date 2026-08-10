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
@Schema(description = "Sentiment analysis response")
public class SentimentResponse {
    @Schema(example = "POSITIVE")
    private String sentiment;

    @Schema(example = "0.85")
    private int positiveScore;

    @Schema(example = "0.15")
    private int negativeScore;
}
