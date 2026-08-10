package com.hostel.dto;

import com.hostel.entity.AuditLog;
import com.hostel.entity.AuditSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private LocalDateTime timestamp;
    private String action;
    private AuditSeverity severity;
    private Long performedByUserId;
    private String performedByEmail;
    private String performedByRole;
    private String targetType;
    private Long targetId;
    private String description;
    private String ipAddress;
    private String userAgent;

    public static AuditLogResponse fromEntity(AuditLog log) {
        if (log == null) return null;
        return AuditLogResponse.builder()
                .id(log.getId())
                .timestamp(log.getCreatedAt())
                .action(log.getAction())
                .severity(log.getSeverity() != null ? log.getSeverity() : AuditSeverity.INFO)
                .performedByUserId(log.getPerformedByUserId())
                .performedByEmail(log.getPerformedBy())
                .performedByRole(log.getPerformedByRole())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .description(log.getDetails())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .build();
    }
}
