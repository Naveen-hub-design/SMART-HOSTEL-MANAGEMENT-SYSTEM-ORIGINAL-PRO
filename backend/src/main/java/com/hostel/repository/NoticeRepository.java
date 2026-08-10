package com.hostel.repository;

import com.hostel.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByTargetRoleOrTargetRole(Notice.TargetRole role1, Notice.TargetRole role2);

    @Query("SELECT n FROM Notice n WHERE n.targetRole = 'ALL' OR n.targetRole = :role ORDER BY n.postedAt DESC")
    List<Notice> findNoticesForRole(@Param("role") Notice.TargetRole role);

    @Query("SELECT n FROM Notice n ORDER BY n.postedAt DESC")
    List<Notice> findAllOrderByPostedAtDesc();
}
