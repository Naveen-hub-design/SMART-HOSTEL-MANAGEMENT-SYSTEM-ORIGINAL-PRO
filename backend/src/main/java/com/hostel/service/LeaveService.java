package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.LeaveRequestDto;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.DuplicateResourceException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentRepository studentRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final WardenRepository wardenRepository;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        StudentRepository studentRepository,
                        EmailService emailService,
                        AuditService auditService,
                        UserRepository userRepository,
                        WardenRepository wardenRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.studentRepository = studentRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.wardenRepository = wardenRepository;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User getCurrentUser() {
        String currentUserEmail = getCurrentUserEmail();
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));
    }

    private void verifyWardenBlockAccess(Long wardenUserId, LeaveRequest leaveRequest) {

        Warden warden = wardenRepository.findByUserId(wardenUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Warden not found for userId: " + wardenUserId));

        HostelBlock wardenBlock = warden.getBlock();

        if (wardenBlock == null) {
            throw new AccessDeniedException(
                    "Warden is not assigned to a hostel block");
        }

        Student student = leaveRequest.getStudent();

        if (student == null ||
                student.getRoom() == null ||
                student.getRoom().getBlock() == null) {

            throw new AccessDeniedException(
                    "Student is not assigned to a hostel block");
        }

        Long studentBlockId = student.getRoom().getBlock().getId();

        if (!wardenBlock.getId().equals(studentBlockId)) {
            throw new AccessDeniedException(
                    "You are not authorized to access this leave request");
        }
    }

    public ApiResponse<Void> applyLeave(Long userId, LeaveRequestDto leaveRequestDto) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        validateLeaveDates(student.getId(), leaveRequestDto.getFromDate(),
                leaveRequestDto.getToDate());

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .student(student)
                .fromDate(leaveRequestDto.getFromDate().trim())
                .toDate(leaveRequestDto.getToDate().trim())
                .reason(leaveRequestDto.getReason())
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        leaveRequestRepository.save(leaveRequest);

        auditService.logAction("LEAVE_APPLIED", getCurrentUserEmail(), null, "LEAVE", leaveRequest.getId(),
                "Leave applied from " + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate());
        return ApiResponse.success("Leave applied successfully", null);
    }

    private static LocalDate parseLeaveDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Service-layer leave validation: required ISO dates, ordered range,
     * and no overlap with the student's active (PENDING/APPROVED) leaves.
     * Malformed input is a 400; a genuine scheduling conflict is a 409.
     */
    private void validateLeaveDates(Long studentId, String fromDate, String toDate) {
        LocalDate from = parseLeaveDate(fromDate);
        LocalDate to = parseLeaveDate(toDate);
        if (from == null || to == null) {
            throw new BadRequestException(
                    "Leave from-date and to-date are required in YYYY-MM-DD format");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException(
                    "Leave from-date must be on or before to-date");
        }

        for (LeaveRequest existing :
                leaveRequestRepository.findByStudentId(studentId)) {
            if (existing.getStatus() != LeaveRequest.LeaveStatus.PENDING
                    && existing.getStatus() != LeaveRequest.LeaveStatus.APPROVED) {
                continue;
            }
            LocalDate existingFrom = parseLeaveDate(existing.getFromDate());
            LocalDate existingTo = parseLeaveDate(existing.getToDate());
            if (existingFrom == null || existingTo == null) {
                continue;
            }
            if (!from.isAfter(existingTo) && !existingFrom.isAfter(to)) {
                throw new DuplicateResourceException(
                        "Leave request overlaps with an existing "
                                + existing.getStatus().name()
                                + " leave (" + existing.getFromDate()
                                + " to " + existing.getToDate() + ")");
            }
        }
    }

    public ApiResponse<List<LeaveRequestDto>> getAllLeaves() {

        User currentUser = getCurrentUser();

        List<LeaveRequest> leaves;

        if (currentUser.getRole() == User.Role.WARDEN) {

            Warden warden = wardenRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Warden not found for userId: " + currentUser.getId()));

            if (warden.getBlock() == null) {
                throw new AccessDeniedException(
                        "Warden is not assigned to a hostel block");
            }

            leaves = leaveRequestRepository.findByBlockIdOrderByAppliedAtDesc(
                    warden.getBlock().getId());

        } else if (currentUser.getRole() == User.Role.ADMIN) {

            leaves = leaveRequestRepository.findAllOrderByAppliedAtDesc();

        } else {

            throw new AccessDeniedException(
                    "You are not authorized to view leave requests");
        }

        List<LeaveRequestDto> dtos = leaves.stream()
                .map(LeaveRequestDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<LeaveRequestDto>> getStudentLeaves(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        List<LeaveRequest> leaves = leaveRequestRepository.findByStudentIdOrderByAppliedAtDesc(student.getId());
        List<LeaveRequestDto> dtos = leaves.stream()
                .map(LeaveRequestDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Void> approveLeave(Long leaveId, String wardenName) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", leaveId));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.WARDEN) {
            verifyWardenBlockAccess(currentUser.getId(), leaveRequest);
        }

        leaveRequest.setStatus(LeaveRequest.LeaveStatus.APPROVED);
        leaveRequest.setResolvedAt(LocalDateTime.now());
        leaveRequest.setApprovedBy(wardenName);
        leaveRequestRepository.save(leaveRequest);

        emailService.sendLeaveApproval(
                leaveRequest.getStudent().getUser().getEmail(),
                leaveRequest.getStudent().getUser().getName(),
                "APPROVED",
                leaveRequest.getFromDate().toString(),
                leaveRequest.getToDate().toString(),
                null
        );

        auditService.logAction("LEAVE_APPROVED", getCurrentUserEmail(), null, "LEAVE", leaveRequest.getId(),
                "Leave approved by " + wardenName);
        return ApiResponse.success("Leave approved successfully", null);
    }

    public ApiResponse<Void> rejectLeave(Long leaveId, String remarks, String wardenName) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", leaveId));

        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.WARDEN) {
            verifyWardenBlockAccess(currentUser.getId(), leaveRequest);
        }

        leaveRequest.setStatus(LeaveRequest.LeaveStatus.REJECTED);
        leaveRequest.setResolvedAt(LocalDateTime.now());
        leaveRequest.setApprovedBy(wardenName);
        leaveRequest.setRemarks(remarks);
        leaveRequestRepository.save(leaveRequest);

        emailService.sendLeaveApproval(
                leaveRequest.getStudent().getUser().getEmail(),
                leaveRequest.getStudent().getUser().getName(),
                "REJECTED",
                leaveRequest.getFromDate().toString(),
                leaveRequest.getToDate().toString(),
                remarks
        );

        auditService.logAction("LEAVE_REJECTED", getCurrentUserEmail(), null, "LEAVE", leaveRequest.getId(),
                "Leave rejected by " + wardenName + ": " + remarks);
        return ApiResponse.success("Leave rejected", null);
    }

    public ApiResponse<List<LeaveRequestDto>> getPendingLeaves() {

        User currentUser = getCurrentUser();

        List<LeaveRequest> leaves;

        if (currentUser.getRole() == User.Role.WARDEN) {

            Warden warden = wardenRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Warden not found for userId: " + currentUser.getId()));

            if (warden.getBlock() == null) {
                throw new AccessDeniedException(
                        "Warden is not assigned to a hostel block");
            }

            leaves = leaveRequestRepository.findByBlockIdOrderByAppliedAtDesc(
                            warden.getBlock().getId())
                    .stream()
                    .filter(leave ->
                            leave.getStatus() == LeaveRequest.LeaveStatus.PENDING)
                    .collect(Collectors.toList());

        } else if (currentUser.getRole() == User.Role.ADMIN) {

            leaves = leaveRequestRepository.findByStatusOrderByAppliedAtDesc(
                    LeaveRequest.LeaveStatus.PENDING);

        } else {

            throw new AccessDeniedException(
                    "You are not authorized to view leave requests");
        }

        List<LeaveRequestDto> dtos = leaves.stream()
                .map(LeaveRequestDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Map<String, Long>> getLeaveCountByStatus() {

        User currentUser = getCurrentUser();

        Map<String, Long> counts = new HashMap<>();

        long pending;
        long approved;
        long rejected;

        if (currentUser.getRole() == User.Role.WARDEN) {

            Warden warden = wardenRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Warden not found for userId: " + currentUser.getId()));

            if (warden.getBlock() == null) {
                throw new AccessDeniedException(
                        "Warden is not assigned to a hostel block");
            }

            Long blockId = warden.getBlock().getId();

            pending = leaveRequestRepository.countByBlockIdAndStatus(
                    blockId, LeaveRequest.LeaveStatus.PENDING);
            approved = leaveRequestRepository.countByBlockIdAndStatus(
                    blockId, LeaveRequest.LeaveStatus.APPROVED);
            rejected = leaveRequestRepository.countByBlockIdAndStatus(
                    blockId, LeaveRequest.LeaveStatus.REJECTED);

        } else if (currentUser.getRole() == User.Role.ADMIN) {

            pending = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.PENDING);
            approved = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.APPROVED);
            rejected = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.REJECTED);

        } else {

            throw new AccessDeniedException(
                    "You are not authorized to view leave statistics");
        }

        counts.put("pending", pending);
        counts.put("approved", approved);
        counts.put("rejected", rejected);
        counts.put("total", pending + approved + rejected);

        return ApiResponse.success(counts);
    }
}
