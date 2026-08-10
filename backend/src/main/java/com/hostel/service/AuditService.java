package com.hostel.service;

import com.hostel.entity.AuditAction;
import com.hostel.entity.AuditLog;
import com.hostel.entity.AuditSeverity;
import com.hostel.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog logAction(String action, String performedBy, String performedByRole,
                               String targetType, Long targetId, String details) {
        return logAction(action, AuditSeverity.INFO, null, performedBy, performedByRole, targetType, targetId, details, null, null);
    }

    public AuditLog logAction(AuditAction action, AuditSeverity severity, Long performedByUserId,
                              String performedBy, String performedByRole, String targetType,
                              Long targetId, String details) {
        return logAction(action.name(), severity, performedByUserId, performedBy, performedByRole, targetType, targetId, details, getClientIp(), getUserAgent());
    }

    public AuditLog logAction(String action, AuditSeverity severity, Long performedByUserId,
                              String performedBy, String performedByRole, String targetType,
                              Long targetId, String details, String ipAddress, String userAgent) {
        String actorEmail = (performedBy != null && !performedBy.isBlank()) ? performedBy : getCurrentUserEmail();
        String actorRole = (performedByRole != null && !performedByRole.isBlank()) ? performedByRole : getCurrentUserRole();
        String clientIp = (ipAddress != null && !ipAddress.isBlank()) ? ipAddress : getClientIp();
        String agent = (userAgent != null && !userAgent.isBlank()) ? userAgent : getUserAgent();

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .severity(severity != null ? severity : AuditSeverity.INFO)
                .performedByUserId(performedByUserId)
                .performedBy(actorEmail != null ? actorEmail : "SYSTEM")
                .performedByRole(actorRole != null ? actorRole : "SYSTEM")
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(clientIp)
                .userAgent(agent)
                .build();

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(int page, int size, String search, String action,
                                       String role, String severity, String startDate, String endDate) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                Predicate actionMatch = cb.like(cb.lower(root.get("action")), term);
                Predicate emailMatch = cb.like(cb.lower(root.get("performedBy")), term);
                Predicate targetMatch = cb.like(cb.lower(root.get("targetType")), term);
                Predicate detailsMatch = cb.like(cb.lower(root.get("details")), term);
                predicates.add(cb.or(actionMatch, emailMatch, targetMatch, detailsMatch));
            }

            if (action != null && !action.trim().isEmpty() && !"ALL".equalsIgnoreCase(action)) {
                predicates.add(cb.equal(cb.upper(root.get("action")), action.trim().toUpperCase()));
            }

            if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
                predicates.add(cb.equal(cb.upper(root.get("performedByRole")), role.trim().toUpperCase()));
            }

            if (severity != null && !severity.trim().isEmpty() && !"ALL".equalsIgnoreCase(severity)) {
                try {
                    AuditSeverity sevEnum = AuditSeverity.valueOf(severity.trim().toUpperCase());
                    predicates.add(cb.equal(root.get("severity"), sevEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDateTime start = LocalDateTime.parse(startDate.contains("T") ? startDate : startDate + "T00:00:00");
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
                } catch (Exception ignored) {
                }
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    LocalDateTime end = LocalDateTime.parse(endDate.contains("T") ? endDate : endDate + "T23:59:59");
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
                } catch (Exception ignored) {
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs(int limit) {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByCreatedAtDesc();
        return logs.size() > limit ? logs.subList(0, limit) : logs;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(String email) {
        return auditLogRepository.findByPerformedByOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByRole(String role) {
        return auditLogRepository.findByPerformedByRoleOrderByCreatedAtDesc(role);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) ? auth.getName() : "SYSTEM";
    }

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }
        return "SYSTEM";
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("User-Agent");
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }
}
