package com.hostel.repository;

import com.hostel.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);

    List<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findTop8ByOrderByCreatedAtDesc();

    List<AuditLog> findByPerformedByRoleOrderByCreatedAtDesc(String role);

    Page<AuditLog> findAll(Specification<AuditLog> spec, Pageable pageable);
}
