package com.hostel.repository;

import com.hostel.entity.AttendanceRecord;
import com.hostel.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByStudentIdAndDate(Long studentId, LocalDate date);

    boolean existsByStudentIdAndDate(Long studentId, LocalDate date);

    List<AttendanceRecord> findByStudentId(Long studentId);

    List<AttendanceRecord> findByStudentIdOrderByDateDesc(Long studentId);

    Page<AttendanceRecord> findByStudentId(Long studentId, Pageable pageable);

    List<AttendanceRecord> findByDate(LocalDate date);

    List<AttendanceRecord> findByStatus(AttendanceStatus status);

    @Query(value = """
        SELECT DISTINCT a FROM AttendanceRecord a
        JOIN a.student s
        JOIN s.room r
        JOIN r.block b
        WHERE b.id = :blockId
          AND (:date IS NULL OR a.date = :date)
          AND (:status IS NULL OR a.status = :status)
        ORDER BY a.date DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT a) FROM AttendanceRecord a
        JOIN a.student s
        JOIN s.room r
        JOIN r.block b
        WHERE b.id = :blockId
          AND (:date IS NULL OR a.date = :date)
          AND (:status IS NULL OR a.status = :status)
        """)
    Page<AttendanceRecord> searchByBlock(@Param("blockId") Long blockId,
                                        @Param("date") LocalDate date,
                                        @Param("status") AttendanceStatus status,
                                        Pageable pageable);

    @Query(value = """
        SELECT a FROM AttendanceRecord a
        WHERE (:date IS NULL OR a.date = :date)
          AND (:status IS NULL OR a.status = :status)
        ORDER BY a.date DESC
        """,
        countQuery = """
        SELECT COUNT(a) FROM AttendanceRecord a
        WHERE (:date IS NULL OR a.date = :date)
          AND (:status IS NULL OR a.status = :status)
        """)
    Page<AttendanceRecord> searchAll(@Param("date") LocalDate date,
                                    @Param("status") AttendanceStatus status,
                                    Pageable pageable);

    @Query(value = """
        SELECT a FROM AttendanceRecord a
        JOIN a.student s
        WHERE s.id = :studentId
          AND (:date IS NULL OR a.date = :date)
          AND (:status IS NULL OR a.status = :status)
        ORDER BY a.date DESC
        """,
        countQuery = """
        SELECT COUNT(a) FROM AttendanceRecord a
        JOIN a.student s
        WHERE s.id = :studentId
          AND (:date IS NULL OR a.date = :date)
          AND (:status IS NULL OR a.status = :status)
        """)
    Page<AttendanceRecord> searchByStudent(@Param("studentId") Long studentId,
                                          @Param("date") LocalDate date,
                                          @Param("status") AttendanceStatus status,
                                          Pageable pageable);
}
