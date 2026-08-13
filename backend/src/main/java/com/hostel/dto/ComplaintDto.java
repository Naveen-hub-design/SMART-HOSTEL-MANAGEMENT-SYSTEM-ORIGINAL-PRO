package com.hostel.dto;

import com.hostel.entity.Complaint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Complaint details")
public class ComplaintDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "1")
    private Long studentId;

    @Schema(example = "John Doe")
    private String studentName;

    @NotBlank(message = "Title is required")
    @Schema(example = "Broken fan")
    private String title;

    @NotBlank(message = "Description is required")
    @Schema(example = "The ceiling fan in room A101 is not working")
    private String description;

    @Schema(example = "https://example.com/fan.jpg")
    private String imageUrl;

    @Schema(example = "ELECTRICAL")
    private String category;

    @Schema(example = "PENDING")
    private String status;

    @Schema(example = "2024-02-28T10:30:00")
    private LocalDateTime createdAt;

    @Schema(example = "2024-03-01T14:00:00")
    private LocalDateTime resolvedAt;

    @Schema(example = "INTERNET", description = "AI-detected category")
    private String aiCategory;

    @Schema(example = "0.95", description = "AI classification confidence")
    private Double aiConfidence;

    @Schema(example = "NEGATIVE", description = "AI-detected sentiment")
    private String sentiment;

    @Schema(example = "HIGH", description = "AI-assessed priority")
    private String priority;

    @Schema(example = "High priority - Verify the network connection...", description = "AI-generated recommended action")
    private String aiRecommendation;

    public static ComplaintDto fromEntity(Complaint complaint) {
        return ComplaintDto.builder()
                .id(complaint.getId())
                .studentId(complaint.getStudent().getId())
                .studentName(complaint.getStudent().getUser().getName())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .imageUrl(complaint.getImageUrl())
                .category(complaint.getCategory().name())
                .status(complaint.getStatus().name())
                .createdAt(complaint.getCreatedAt())
                .resolvedAt(complaint.getResolvedAt())
                .aiCategory(complaint.getAiCategory())
                .aiConfidence(complaint.getAiConfidence())
                .sentiment(complaint.getSentiment() != null ? complaint.getSentiment().name() : null)
                .priority(complaint.getPriority() != null ? complaint.getPriority().name() : null)
                .aiRecommendation(complaint.getAiRecommendation())
                .build();
    }
}
