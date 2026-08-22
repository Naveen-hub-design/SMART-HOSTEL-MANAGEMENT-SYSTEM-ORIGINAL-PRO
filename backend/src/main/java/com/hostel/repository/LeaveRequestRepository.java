package com.hostel.repository;

import com.hostel.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByStudentId(Long studentId);
    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByStudentIdOrderByAppliedAtDesc(Long studentId);

    @Query("SELECT l FROM LeaveRequest l ORDER BY l.appliedAt DESC")
    List<LeaveRequest> findAllOrderByAppliedAtDesc();

    long countByStatus(LeaveRequest.LeaveStatus status);
    long countByAppliedAtBetween(LocalDateTime start, LocalDateTime end);
    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(LeaveRequest.LeaveStatus status);

    @Query("""
        SELECT l FROM LeaveRequest l
        JOIN l.student s
        JOIN s.room r
        JOIN r.block b
        WHERE b.id = :blockId
        ORDER BY l.appliedAt DESC
    """)
    List<LeaveRequest> findByBlockIdOrderByAppliedAtDesc(@Param("blockId") Long blockId);

    @Query("""
        SELECT COUNT(l) FROM LeaveRequest l
        JOIN l.student s
        JOIN s.room r
        JOIN r.block b
        WHERE b.id = :blockId
          AND l.status = :status
    """)
    long countByBlockIdAndStatus(
        @Param("blockId") Long blockId,
        @Param("status") LeaveRequest.LeaveStatus status
    );
}
