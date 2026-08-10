package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.PasswordChangeRequest;
import com.hostel.dto.RoomDto;
import com.hostel.dto.StudentProfileDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.FileUploadService;
import com.hostel.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Student", description = "Student profile and settings management")
public class StudentController {

    private final StudentService studentService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    public StudentController(StudentService studentService,
                             UserRepository userRepository,
                             FileUploadService fileUploadService) {
        this.studentService = studentService;
        this.userRepository = userRepository;
        this.fileUploadService = fileUploadService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/profile")
    @Operation(summary = "Get student profile")
    public ResponseEntity<ApiResponse<StudentProfileDto>> getProfile() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(studentService.getProfile(userId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/profile")
    @Operation(summary = "Update student profile")
    public ResponseEntity<ApiResponse<StudentProfileDto>> updateProfile(@Valid @RequestBody StudentProfileDto profileDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(studentService.updateProfile(userId, profileDto));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my-room")
    @Operation(summary = "Get assigned room details")
    public ResponseEntity<ApiResponse<RoomDto>> getMyRoom() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(studentService.getMyRoom(userId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(studentService.changePassword(userId, request));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile picture")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        String imageUrl = fileUploadService.uploadFile(file);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Profile picture uploaded", imageUrl));
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return user.getId();
    }
}
