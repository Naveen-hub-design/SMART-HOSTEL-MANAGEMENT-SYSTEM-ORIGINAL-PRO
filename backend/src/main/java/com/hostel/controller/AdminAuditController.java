package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.AuditLogResponse;
import com.hostel.dto.PageResponse;
import com.hostel.entity.AuditLog;
import com.hostel.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@CrossOrigin(origins = "http://localhost:3000")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Audit Logs", description = "Canonical admin endpoints for auditing system logs")
public class AdminAuditController {

    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Get paginated audit logs", description = "Returns server-side paginated and filtered audit logs for admin audit trail")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        Page<AuditLog> auditLogPage = auditService.getAuditLogs(page, size, search, action, role, severity, startDate, endDate);
        PageResponse<AuditLogResponse> pageResponse = PageResponse.fromPage(auditLogPage, AuditLogResponse::fromEntity);

        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }
}
