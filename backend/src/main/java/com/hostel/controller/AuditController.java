package com.hostel.controller;

import com.hostel.entity.AuditLog;
import com.hostel.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/logs")
    public List<AuditLog> getRecentLogs(@RequestParam(defaultValue = "50") int limit) {
        return auditService.getRecentLogs(limit);
    }

    @GetMapping("/logs/user/{email}")
    public List<AuditLog> getLogsByUser(@PathVariable String email) {
        return auditService.getLogsByUser(email);
    }

    @GetMapping("/logs/role/{role}")
    public List<AuditLog> getLogsByRole(@PathVariable String role) {
        return auditService.getLogsByRole(role);
    }
}
