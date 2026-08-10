package com.hostel.dto;

import com.hostel.entity.LeaveRequest;
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
@Schema(description = "Leave request details")
public class LeaveRequestDto {
    @Schema(example = "1")
    private Long id;

    @Schema(example = "1")
    private Long studentId;

    @Schema(example = "John Doe")
    private String studentName;

    @Schema(example = "ENR2024001")
    private String enrollmentNo;

    @Schema(example = "2024-03-01")
    private String fromDate;

    @Schema(example = "2024-03-05")
    private String toDate;

    @Schema(example = "Going home for vacation")
    private String reason;

    @Schema(example = "PENDING")
    private String status;

    @Schema(example = "2024-02-28T10:30:00")
    private LocalDateTime appliedAt;

    @Schema(example = "2024-03-01T14:00:00")
    private LocalDateTime resolvedAt;

    @Schema(example = "Warden")
    private String approvedBy;

    @Schema(example = "Approved")
    private String remarks;

    public static LeaveRequestDto fromEntity(LeaveRequest leaveRequest) {
        return LeaveRequestDto.builder()
                .id(leaveRequest.getId())
                .studentId(leaveRequest.getStudent().getId())
                .studentName(leaveRequest.getStudent().getUser().getName())
                .enrollmentNo(leaveRequest.getStudent().getEnrollmentNo())
                .fromDate(leaveRequest.getFromDate())
                .toDate(leaveRequest.getToDate())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus().name())
                .appliedAt(leaveRequest.getAppliedAt())
                .resolvedAt(leaveRequest.getResolvedAt())
                .approvedBy(leaveRequest.getApprovedBy())
                .remarks(leaveRequest.getRemarks())
                .build();
    }
}
