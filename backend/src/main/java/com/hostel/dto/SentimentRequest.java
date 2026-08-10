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
@Schema(description = "Sentiment analysis request")
public class SentimentRequest {
    @Schema(example = "The food is delicious and clean", description = "Feedback text to analyze")
    private String feedbackText;
}
