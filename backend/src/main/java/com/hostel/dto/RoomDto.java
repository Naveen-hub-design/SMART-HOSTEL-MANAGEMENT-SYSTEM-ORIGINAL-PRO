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
@Schema(description = "Room details")
public class RoomDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "A101")
    private String roomNo;

    @Schema(example = "A-Block")
    private String blockName;

    @Schema(example = "1")
    private Long blockId;

    @Schema(example = "1")
    private Integer floor;

    @Schema(example = "4")
    private Integer capacity;

    @Schema(example = "3")
    private Integer occupants;

    @Schema(example = "AVAILABLE")
    private String status;

    @Schema(example = "5000.0")
    private Double rent;
}
