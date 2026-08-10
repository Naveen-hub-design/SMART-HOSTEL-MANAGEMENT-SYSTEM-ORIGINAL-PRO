package com.hostel.dto;

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
@Schema(description = "Notice details")
public class NoticeDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "Holiday Notice")
    private String title;

    @Schema(example = "All students are advised that the hostel will remain open during the holidays")
    private String content;

    @Schema(example = "Admin")
    private String postedBy;

    @Schema(example = "2024-02-28T10:30:00")
    private LocalDateTime postedAt;

    @Schema(example = "2024-03-15T23:59:59")
    private LocalDateTime expiresAt;

    @Schema(example = "ALL")
    private String targetRole;
}
