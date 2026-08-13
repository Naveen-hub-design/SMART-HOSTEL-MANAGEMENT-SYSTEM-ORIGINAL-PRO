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
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @Column(length = 50)
    private String aiCategory;

    private Double aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessFeedback.Sentiment sentiment;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ComplaintPriority priority;

    @Column(columnDefinition = "TEXT")
    private String aiRecommendation;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ComplaintStatus.PENDING;
        }
    }

    public enum ComplaintCategory {
        ELECTRICAL, PLUMBING, INTERNET, FURNITURE, MESS, GENERAL
    }

    public enum ComplaintStatus {
        PENDING, IN_PROGRESS, RESOLVED, REJECTED
    }

    public enum ComplaintPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
