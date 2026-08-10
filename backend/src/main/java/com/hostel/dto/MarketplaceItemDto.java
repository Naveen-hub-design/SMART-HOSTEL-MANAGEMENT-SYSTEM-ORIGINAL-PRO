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
@Schema(description = "Marketplace item details")
public class MarketplaceItemDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "Used Textbook")
    private String title;

    @Schema(example = "Computer Science textbook in good condition")
    private String description;

    @Schema(example = "250.0")
    private Double price;

    @Schema(example = "https://example.com/book.jpg")
    private String imageUrl;

    @Schema(example = "BOOKS")
    private String category;

    @Schema(example = "John Doe")
    private String sellerName;

    @Schema(example = "AVAILABLE")
    private String status;

    @Schema(example = "2024-02-28T10:30:00")
    private LocalDateTime createdAt;
}
