package com.hostel.repository;

import com.hostel.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStudentId(Long studentId);
    List<Complaint> findByStatus(Complaint.ComplaintStatus status);
    List<Complaint> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT c FROM Complaint c JOIN c.student s JOIN s.room r JOIN r.block b WHERE b.id = :blockId ORDER BY c.createdAt DESC")
    List<Complaint> findByBlockIdOrderByCreatedAtDesc(@Param("blockId") Long blockId);

    @Query("SELECT c FROM Complaint c ORDER BY c.createdAt DESC")
    List<Complaint> findAllOrderByCreatedAtDesc();

    long countByStatus(Complaint.ComplaintStatus status);
}
