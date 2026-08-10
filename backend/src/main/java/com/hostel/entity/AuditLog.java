package com.hostel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_performed_by", columnList = "performedBy"),
    @Index(name = "idx_audit_performed_by_user_id", columnList = "performedByUserId"),
    @Index(name = "idx_audit_severity", columnList = "severity"),
    @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuditSeverity severity = AuditSeverity.INFO;

    private Long performedByUserId;

    @Column(nullable = false)
    private String performedBy;

    private String performedByRole;

    private String targetType;

    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String details;

    private String ipAddress;

    private String userAgent;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.severity == null) {
            this.severity = AuditSeverity.INFO;
        }
    }
}
