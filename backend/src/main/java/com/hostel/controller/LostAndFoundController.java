package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.LostAndFoundDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.LostAndFoundService;
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

@RestController
@RequestMapping("/api/lost-found")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Lost & Found", description = "Lost and found item reporting")
public class LostAndFoundController {

    private final LostAndFoundService lostAndFoundService;
    private final UserRepository userRepository;

    public LostAndFoundController(LostAndFoundService lostAndFoundService,
                                  UserRepository userRepository) {
        this.lostAndFoundService = lostAndFoundService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Report lost/found item")
    public ResponseEntity<ApiResponse<Void>> reportItem(
            @ModelAttribute LostAndFoundDto lostAndFoundDto,
            @RequestParam(required = false) MultipartFile image) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(lostAndFoundService.reportItem(userId, lostAndFoundDto, image));
    }

    @GetMapping
    @Operation(summary = "Get all items")
    public ResponseEntity<ApiResponse<List<LostAndFoundDto>>> getAllItems() {
        return ResponseEntity.ok(lostAndFoundService.getAllItems());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    @Operation(summary = "Get my reports")
    public ResponseEntity<ApiResponse<List<LostAndFoundDto>>> getMyReports() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(lostAndFoundService.getMyReports(userId));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PutMapping("/{id}/status")
    @Operation(summary = "Update item status")
    public ResponseEntity<ApiResponse<Void>> updateItemStatus(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @RequestParam(required = false) String status) {
        String newStatus = (body != null && body.containsKey("status")) ? body.get("status") : status;
        return ResponseEntity.ok(lostAndFoundService.updateItemStatus(id, newStatus));
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
