package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.NoticeDto;
import com.hostel.entity.Notice;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.NoticeRepository;
import com.hostel.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NoticeService(NoticeRepository noticeRepository,
                         UserRepository userRepository,
                         EmailService emailService) {
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    private User getCurrentUser() {
        String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));
    }

    /**
     * Only ADMIN (global) or the creating user may manage a notice.
     * Legacy notices (createdBy == null) are ADMIN-only. Ownership is
     * resolved exclusively from the server-side createdBy relationship —
     * never from postedBy display text or client input.
     */
    private void verifyNoticeManageAccess(User currentUser, Notice notice) {
        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }
        if (notice.getCreatedBy() != null
                && notice.getCreatedBy().getId() != null
                && notice.getCreatedBy().getId()
                        .equals(currentUser.getId())) {
            return;
        }
        throw new AccessDeniedException(
                "You are not authorized to manage this notice");
    }

    public ApiResponse<Void> createNotice(Long userId, NoticeDto noticeDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Notice.TargetRole targetRole = Notice.TargetRole.ALL;
        if (noticeDto.getTargetRole() != null) {
            try {
                targetRole = Notice.TargetRole.valueOf(noticeDto.getTargetRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                targetRole = Notice.TargetRole.ALL;
            }
        }

        Notice notice = Notice.builder()
                .title(noticeDto.getTitle())
                .content(noticeDto.getContent())
                .postedBy(user.getName())
                .createdBy(user)
                .expiresAt(noticeDto.getExpiresAt())
                .targetRole(targetRole)
                .build();
        noticeRepository.save(notice);

        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (targetRole == Notice.TargetRole.ALL
                    || (targetRole == Notice.TargetRole.STUDENT && u.getRole() == User.Role.STUDENT)
                    || (targetRole == Notice.TargetRole.WARDEN && u.getRole() == User.Role.WARDEN)) {
                emailService.sendNewNotice(u.getEmail(), notice.getTitle(), notice.getContent());
            }
        }

        return ApiResponse.success("Notice created successfully", null);
    }

    public ApiResponse<Void> updateNotice(Long noticeId, NoticeDto noticeDto) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", noticeId));

        verifyNoticeManageAccess(getCurrentUser(), notice);

        notice.setTitle(noticeDto.getTitle());
        notice.setContent(noticeDto.getContent());
        notice.setExpiresAt(noticeDto.getExpiresAt());
        if (noticeDto.getTargetRole() != null) {
            notice.setTargetRole(Notice.TargetRole.valueOf(noticeDto.getTargetRole().toUpperCase()));
        }
        noticeRepository.save(notice);

        return ApiResponse.success("Notice updated successfully", null);
    }

    public ApiResponse<Void> deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice", noticeId));

        verifyNoticeManageAccess(getCurrentUser(), notice);

        noticeRepository.delete(notice);
        return ApiResponse.success("Notice deleted successfully", null);
    }

    public ApiResponse<List<NoticeDto>> getAllNotices() {
        List<Notice> notices = noticeRepository.findAllOrderByPostedAtDesc();
        List<NoticeDto> dtos = notices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<NoticeDto>> getNoticesForRole(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            role = "ALL";
        }
        Notice.TargetRole targetRole = Notice.TargetRole.valueOf(role.toUpperCase());
        List<Notice> notices = noticeRepository.findNoticesForRole(targetRole);
        List<NoticeDto> dtos = notices.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    private NoticeDto mapToDto(Notice notice) {
        return NoticeDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .postedBy(notice.getPostedBy())
                .postedAt(notice.getPostedAt())
                .expiresAt(notice.getExpiresAt())
                .targetRole(notice.getTargetRole().name())
                .build();
    }
}
