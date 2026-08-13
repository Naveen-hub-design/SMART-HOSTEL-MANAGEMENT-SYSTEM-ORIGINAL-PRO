package com.hostel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard statistics")
public class DashboardStatsDto {
    @Schema(example = "150")
    private long totalStudents;

    @Schema(example = "10")
    private long totalWardens;

    @Schema(example = "5")
    private long totalBlocks;

    @Schema(example = "80")
    private long monthlyLeaves;

    @Schema(example = "100")
    private long totalRooms;

    @Schema(example = "80")
    private long occupiedRooms;

    @Schema(example = "20")
    private long availableRooms;

    @Schema(example = "45")
    private long totalComplaints;

    @Schema(example = "10")
    private long pendingComplaints;

    @Schema(example = "35")
    private long resolvedComplaints;

    @Schema(example = "25")
    private long totalLeaves;

    @Schema(example = "5")
    private long pendingLeaves;

    @Schema(example = "20")
    private long approvedLeaves;

    @Schema(description = "Per-block occupancy statistics")
    @Builder.Default
    private List<BlockStatsDto> blockStats = new ArrayList<>();

    @Schema(description = "Most recent admin activities")
    @Builder.Default
    private List<RecentActivityDto> recentActivities = new ArrayList<>();
}
