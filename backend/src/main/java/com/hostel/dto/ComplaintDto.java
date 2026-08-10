package com.hostel.dto;

import com.hostel.entity.Complaint;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(example = "Broken fan")
    private String title;

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
                .build();
    }
}
