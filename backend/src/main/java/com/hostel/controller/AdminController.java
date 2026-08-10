package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.AuthResponse;
import com.hostel.dto.DashboardStatsDto;
import com.hostel.dto.RegisterRequest;
import com.hostel.dto.StudentProfileDto;
import com.hostel.service.AdminService;
import com.hostel.service.ComplaintService;
import com.hostel.service.LeaveService;
import com.hostel.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Admin", description = "Administrator operations")
public class AdminController {

    private final AdminService adminService;
    private final StudentService studentService;
    private final LeaveService leaveService;
    private final ComplaintService complaintService;

    public AdminController(AdminService adminService,
                           StudentService studentService,
                           LeaveService leaveService,
                           ComplaintService complaintService) {
        this.adminService = adminService;
        this.studentService = studentService;
        this.leaveService = leaveService;
        this.complaintService = complaintService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/warden")
    @Operation(summary = "Create new warden")
    public ResponseEntity<ApiResponse<AuthResponse>> createWarden(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(adminService.createWarden(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/warden/{id}")
    @Operation(summary = "Delete warden")
    public ResponseEntity<ApiResponse<Void>> deleteWarden(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteWarden(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/hostel-block")
    @Operation(summary = "Create hostel block")
    public ResponseEntity<ApiResponse<Void>> createHostelBlock(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String address) {
        String blockName = (body != null && body.containsKey("name")) ? body.get("name") : name;
        String blockCode = (body != null && body.containsKey("code")) ? body.get("code") : code;
        String blockAddress = (body != null && body.containsKey("address")) ? body.get("address") : address;
        return ResponseEntity.ok(adminService.createHostelBlock(blockName, blockCode, blockAddress));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/hostel-blocks")
    @Operation(summary = "Get all hostel blocks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHostelBlocks() {
        return ResponseEntity.ok(adminService.getAllHostelBlocks());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/students")
    @Operation(summary = "Get all students")
    public ResponseEntity<ApiResponse<List<StudentProfileDto>>> getStudents() {
        return ResponseEntity.ok(adminService.getAllStudents());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping({"/students/{id}", "/student/{id}"})
    @Operation(summary = "Delete student")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteStudent(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reports")
    @Operation(summary = "Get system reports")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReports() {
        Map<String, Long> leaveStats = leaveService.getLeaveCountByStatus().getData();
        Map<String, Long> complaintStats = complaintService.getComplaintCountByStatus().getData();
        Map<String, Object> reports = Map.of(
                "leaves", leaveStats,
                "complaints", complaintStats
        );
        return ResponseEntity.ok(ApiResponse.success(reports));
    }
}
