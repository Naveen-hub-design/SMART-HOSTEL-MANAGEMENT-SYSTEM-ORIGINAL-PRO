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
@Schema(description = "Occupancy statistics for a single hostel block")
public class BlockStatsDto {
    @Schema(example = "A Wing - Senior Boys")
    private String name;

    @Schema(example = "9")
    private long capacity;

    @Schema(example = "7")
    private long occupied;
}
