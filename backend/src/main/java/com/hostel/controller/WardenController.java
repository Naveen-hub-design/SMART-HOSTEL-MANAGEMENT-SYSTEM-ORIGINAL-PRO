package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.DashboardStatsDto;
import com.hostel.dto.StudentProfileDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.WardenService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wardens")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Warden", description = "Warden operations and dashboard")
public class WardenController {

    private final WardenService wardenService;
    private final UserRepository userRepository;

    public WardenController(WardenService wardenService,
                            UserRepository userRepository) {
        this.wardenService = wardenService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assign-block/{wardenId}/{blockId}")
    @Operation(summary = "Assign warden to hostel block")
    public ResponseEntity<ApiResponse<Void>> assignWardenToBlock(@PathVariable Long wardenId,
                                                                  @PathVariable Long blockId) {
        return ResponseEntity.ok(wardenService.assignWardenToBlock(wardenId, blockId));
    }

    @PreAuthorize("hasRole('WARDEN')")
    @GetMapping("/dashboard")
    @Operation(summary = "Get warden dashboard stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboard() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(wardenService.getDashboardStats(userId));
    }

    @PreAuthorize("hasRole('WARDEN')")
    @GetMapping("/students")
    @Operation(summary = "Get students under warden")
    public ResponseEntity<ApiResponse<List<StudentProfileDto>>> getStudents() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(wardenService.getStudentsByWardenBlock(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all wardens")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllWardens() {
        return ResponseEntity.ok(wardenService.getAllWardens());
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
