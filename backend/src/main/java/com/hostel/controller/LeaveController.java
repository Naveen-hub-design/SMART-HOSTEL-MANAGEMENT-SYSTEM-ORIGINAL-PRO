package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.LeaveRequestDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Leave Management", description = "Leave application and approval workflow")
public class LeaveController {

    private final LeaveService leaveService;
    private final UserRepository userRepository;

    public LeaveController(LeaveService leaveService,
                           UserRepository userRepository) {
        this.leaveService = leaveService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    @Operation(summary = "Apply for leave")
    public ResponseEntity<ApiResponse<Void>> applyLeave(@Valid @RequestBody LeaveRequestDto leaveRequestDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(leaveService.applyLeave(userId, leaveRequestDto));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping
    @Operation(summary = "Get all leave requests")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    @Operation(summary = "Get my leave requests")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getMyLeaves() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(leaveService.getStudentLeaves(userId));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve leave request")
    public ResponseEntity<ApiResponse<Void>> approveLeave(@PathVariable Long id) {
        String currentUserEmail = getCurrentUserEmail();
        return ResponseEntity.ok(leaveService.approveLeave(id, currentUserEmail));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject leave request")
    public ResponseEntity<ApiResponse<Void>> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String remarks) {
        String newRemarks = (body != null && body.containsKey("remarks")) ? body.get("remarks") : (remarks != null ? remarks : "Rejected");
        String currentUserEmail = getCurrentUserEmail();
        return ResponseEntity.ok(leaveService.rejectLeave(id, newRemarks, currentUserEmail));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @GetMapping("/pending")
    @Operation(summary = "Get pending leave requests")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping("/stats")
    @Operation(summary = "Get leave statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLeaveStats() {
        return ResponseEntity.ok(leaveService.getLeaveCountByStatus());
    }

    private Long getCurrentUserId() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return user.getId();
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
