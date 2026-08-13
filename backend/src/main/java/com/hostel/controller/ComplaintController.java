package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.ComplaintDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Complaints", description = "Complaint filing and resolution")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    public ComplaintController(ComplaintService complaintService,
                               UserRepository userRepository) {
        this.complaintService = complaintService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a complaint")
    public ResponseEntity<ApiResponse<Void>> createComplaint(
            @Valid @ModelAttribute ComplaintDto complaintDto,
            @RequestParam(required = false) MultipartFile image) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(complaintService.createComplaint(userId, complaintDto, image));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping
    @Operation(summary = "Get all complaints")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    @Operation(summary = "Get my complaints")
    public ResponseEntity<ApiResponse<List<ComplaintDto>>> getMyComplaints() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(complaintService.getStudentComplaints(userId));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PutMapping("/{id}/status")
    @Operation(summary = "Update complaint status")
    public ResponseEntity<ApiResponse<Void>> updateComplaintStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) String status) {
        String newStatus = (body != null && body.containsKey("status")) ? body.get("status") : status;
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, newStatus));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping("/stats")
    @Operation(summary = "Get complaint statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getComplaintStats() {
        return ResponseEntity.ok(complaintService.getComplaintCountByStatus());
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
