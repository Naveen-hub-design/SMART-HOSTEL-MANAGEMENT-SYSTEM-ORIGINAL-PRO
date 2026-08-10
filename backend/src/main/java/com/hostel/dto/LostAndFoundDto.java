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
@Schema(description = "Lost and found item details")
public class LostAndFoundDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "Black Wallet")
    private String title;

    @Schema(example = "Black leather wallet found in the mess hall")
    private String description;

    @Schema(example = "https://example.com/wallet.jpg")
    private String imageUrl;

    @Schema(example = "FOUND")
    private String status;

    @Schema(example = "ACCESSORIES")
    private String category;

    @Schema(example = "Mess Hall")
    private String location;

    @Schema(example = "9999999999")
    private String contactInfo;

    @Schema(example = "John Doe")
    private String reporterName;

    @Schema(example = "2024-02-28T10:30:00")
    private LocalDateTime createdAt;
}
