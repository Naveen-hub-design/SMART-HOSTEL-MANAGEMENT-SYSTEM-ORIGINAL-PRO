package com.hostel.controller;

import com.hostel.dto.AICategorizationRequest;
import com.hostel.dto.AICategorizationResponse;
import com.hostel.dto.ApiResponse;
import com.hostel.dto.RoomDto;
import com.hostel.dto.SentimentRequest;
import com.hostel.dto.SentimentResponse;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.AIService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Features", description = "AI-powered complaint categorization and sentiment analysis")
public class AIController {

    private final AIService aiService;
    private final UserRepository userRepository;

    public AIController(AIService aiService,
                        UserRepository userRepository) {
        this.aiService = aiService;
        this.userRepository = userRepository;
    }

    @PostMapping("/categorize-complaint")
    @Operation(summary = "Categorize complaint", description = "Uses keyword-based AI to classify complaints into ELECTRICAL, PLUMBING, INTERNET, FURNITURE, MESS, or GENERAL")
    public ResponseEntity<ApiResponse<AICategorizationResponse>> categorizeComplaint(
            @RequestBody AICategorizationRequest request) {
        AICategorizationResponse response = aiService.categorizeComplaint(
                request.getComplaintTitle(), request.getComplaintDescription());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/analyze-sentiment")
    @Operation(summary = "Analyze sentiment", description = "Analyzes mess feedback text for positive/negative sentiment")
    public ResponseEntity<ApiResponse<SentimentResponse>> analyzeSentiment(
            @RequestBody SentimentRequest request) {
        String sentiment = aiService.analyzeSentiment(request.getFeedbackText());
        SentimentResponse response = SentimentResponse.builder()
                .sentiment(sentiment)
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping({"/recommend-room", "/recommended-rooms"})
    @Operation(summary = "Recommend rooms", description = "AI-powered room recommendations based on student preferences and occupancy")
    public ResponseEntity<ApiResponse<List<RoomDto>>> recommendRoom(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<RoomDto> recommendations = aiService.recommendRoom(userId);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userDetails.getUsername()));
        return user.getId();
    }
}
