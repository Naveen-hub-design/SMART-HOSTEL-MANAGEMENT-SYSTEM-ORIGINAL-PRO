package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.NoticeDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Notices", description = "Notice board management")
public class NoticeController {

    private final NoticeService noticeService;
    private final UserRepository userRepository;

    public NoticeController(NoticeService noticeService,
                            UserRepository userRepository) {
        this.noticeService = noticeService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PostMapping
    @Operation(summary = "Create notice")
    public ResponseEntity<ApiResponse<Void>> createNotice(@Valid @RequestBody NoticeDto noticeDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(noticeService.createNotice(userId, noticeDto));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update notice")
    public ResponseEntity<ApiResponse<Void>> updateNotice(@PathVariable Long id,
                                                           @Valid @RequestBody NoticeDto noticeDto) {
        return ResponseEntity.ok(noticeService.updateNotice(id, noticeDto));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notice")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.deleteNotice(id));
    }

    @GetMapping
    @Operation(summary = "Get notices for user role")
    public ResponseEntity<ApiResponse<List<NoticeDto>>> getNotices() {
        String role = extractRole();
        return ResponseEntity.ok(noticeService.getNoticesForRole(role));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping("/all")
    @Operation(summary = "Get all notices")
    public ResponseEntity<ApiResponse<List<NoticeDto>>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNotices());
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

    private String extractRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("ALL");
    }
}
