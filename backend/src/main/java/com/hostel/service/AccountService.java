package com.hostel.service;

import com.hostel.dto.AccountProfileDto;
import com.hostel.dto.ApiResponse;
import com.hostel.dto.PasswordChangeRequest;
import com.hostel.entity.Admin;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.AdminRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Self-service account management for wardens and admins.
 *
 * Identity always comes from the authenticated JWT via the
 * SecurityContext. No endpoint here accepts another user's ID, so a
 * caller can only ever read or modify their own account. Students keep
 * using the existing StudentController endpoints.
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final WardenRepository wardenRepository;
    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final FileUploadService fileUploadService;

    public AccountService(UserRepository userRepository,
                          WardenRepository wardenRepository,
                          AdminRepository adminRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          AuditService auditService,
                          FileUploadService fileUploadService) {
        this.userRepository = userRepository;
        this.wardenRepository = wardenRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.fileUploadService = fileUploadService;
    }

    private User getCurrentUser() {
        String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));
    }

    private void requireStaff(User currentUser) {
        if (currentUser.getRole() != User.Role.WARDEN
                && currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException(
                    "You are not authorized to access account settings");
        }
    }

    public ApiResponse<AccountProfileDto> getProfile() {
        User currentUser = getCurrentUser();
        requireStaff(currentUser);
        return ApiResponse.success(toDto(currentUser));
    }

    public ApiResponse<AccountProfileDto> updateProfile(AccountProfileDto dto) {
        User currentUser = getCurrentUser();
        requireStaff(currentUser);

        // Only name and phone are mutable here. Email, role, block,
        // qualification and department are intentionally ignored even
        // if a client sends them.
        if (dto.getName() != null && !dto.getName().isBlank()) {
            currentUser.setName(dto.getName().trim());
        }
        if (dto.getPhone() != null) {
            currentUser.setPhone(dto.getPhone().trim());
        }
        userRepository.save(currentUser);

        auditService.logAction("PROFILE_UPDATED", currentUser.getEmail(),
                currentUser.getRole().name(), "USER", currentUser.getId(),
                "Profile updated");
        return ApiResponse.success(
                "Profile updated successfully", toDto(currentUser));
    }

    public ApiResponse<Void> changePassword(PasswordChangeRequest request) {
        User currentUser = getCurrentUser();
        requireStaff(currentUser);

        if (request.getCurrentPassword() == null
                || !passwordEncoder.matches(request.getCurrentPassword(),
                        currentUser.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.getNewPassword() == null
                || request.getNewPassword().length() < 6) {
            throw new BadRequestException(
                    "New password must be at least 6 characters");
        }

        currentUser.setPassword(
                passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        auditService.logAction("PASSWORD_CHANGED", currentUser.getEmail(),
                currentUser.getRole().name(), "USER", currentUser.getId(),
                "Password changed");
        return ApiResponse.success("Password changed successfully", null);
    }

    /**
     * Profile picture upload for the authenticated user (student, warden
     * or admin). Identity comes only from the JWT; the hardened
     * FileUploadService performs all file validation. The URL is stored
     * on the User row, the single source of truth for display.
     */
    public ApiResponse<String> uploadProfilePicture(MultipartFile file) {
        User currentUser = getCurrentUser();

        String imageUrl = fileUploadService.uploadFile(file);
        currentUser.setProfileImageUrl(imageUrl);
        userRepository.save(currentUser);

        auditService.logAction("PROFILE_PICTURE_UPDATED", currentUser.getEmail(),
                currentUser.getRole().name(), "USER", currentUser.getId(),
                "Profile picture updated");
        return ApiResponse.success("Profile picture uploaded", imageUrl);
    }

    private AccountProfileDto toDto(User user) {
        String qualification = null;
        String department = null;
        String blockName = null;

        if (user.getRole() == User.Role.WARDEN) {
            Optional<Warden> warden =
                    wardenRepository.findByUserId(user.getId());
            if (warden.isPresent()) {
                qualification = warden.get().getQualification();
                if (warden.get().getBlock() != null) {
                    blockName = warden.get().getBlock().getName();
                }
            }
        } else if (user.getRole() == User.Role.ADMIN) {
            Optional<Admin> admin =
                    adminRepository.findByUserId(user.getId());
            if (admin.isPresent()) {
                department = admin.get().getDepartment();
            }
        }

        return AccountProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .qualification(qualification)
                .department(department)
                .blockName(blockName)
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
