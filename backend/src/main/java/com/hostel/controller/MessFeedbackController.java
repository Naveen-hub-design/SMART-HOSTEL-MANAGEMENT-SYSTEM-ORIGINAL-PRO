package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.MessFeedbackDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.MessFeedbackService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mess-feedback")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Mess Feedback", description = "Mess food rating and feedback")
public class MessFeedbackController {

    private final MessFeedbackService messFeedbackService;
    private final UserRepository userRepository;

    public MessFeedbackController(MessFeedbackService messFeedbackService,
                                  UserRepository userRepository) {
        this.messFeedbackService = messFeedbackService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    @Operation(summary = "Submit mess feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(@Valid @RequestBody MessFeedbackDto feedbackDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(messFeedbackService.submitFeedback(userId, feedbackDto));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    @Operation(summary = "Get my feedback")
    public ResponseEntity<ApiResponse<List<MessFeedbackDto>>> getMyFeedback() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(messFeedbackService.getMyFeedback(userId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping("/all")
    @Operation(summary = "Get all feedback")
    public ResponseEntity<ApiResponse<List<MessFeedbackDto>>> getAllFeedback() {
        return ResponseEntity.ok(messFeedbackService.getAllFeedback());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping("/averages")
    @Operation(summary = "Get average ratings")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getAverageRatings() {
        return ResponseEntity.ok(messFeedbackService.getAverageRatings());
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
