package com.hostel.repository;

import com.hostel.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByStudentId(Long studentId);
    List<LeaveRequest> findByStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByStudentIdOrderByAppliedAtDesc(Long studentId);

    @Query("SELECT l FROM LeaveRequest l ORDER BY l.appliedAt DESC")
    List<LeaveRequest> findAllOrderByAppliedAtDesc();

    long countByStatus(LeaveRequest.LeaveStatus status);
    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(LeaveRequest.LeaveStatus status);
}
