package com.hostel.controller;

import com.hostel.dto.AccountProfileDto;
import com.hostel.dto.ApiResponse;
import com.hostel.dto.PasswordChangeRequest;
import com.hostel.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Account", description = "Warden/Admin self-service account management")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @GetMapping("/profile")
    @Operation(summary = "Get own account profile")
    public ResponseEntity<ApiResponse<AccountProfileDto>> getProfile() {
        return ResponseEntity.ok(accountService.getProfile());
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PatchMapping("/profile")
    @Operation(summary = "Update own account profile (name and phone only)")
    public ResponseEntity<ApiResponse<AccountProfileDto>> updateProfile(
            @RequestBody AccountProfileDto dto) {
        return ResponseEntity.ok(accountService.updateProfile(dto));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PutMapping("/password")
    @Operation(summary = "Change own account password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request) {
        return ResponseEntity.ok(accountService.changePassword(request));
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'WARDEN', 'ADMIN')")
    @PutMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload own profile picture")
    public ResponseEntity<ApiResponse<String>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(accountService.uploadProfilePicture(file));
    }
}
