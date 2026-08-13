package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.LeaveRequestDto;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.StudentRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        StudentRepository studentRepository,
                        EmailService emailService,
                        AuditService auditService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.studentRepository = studentRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public ApiResponse<Void> applyLeave(Long userId, LeaveRequestDto leaveRequestDto) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .student(student)
                .fromDate(leaveRequestDto.getFromDate())
                .toDate(leaveRequestDto.getToDate())
                .reason(leaveRequestDto.getReason())
                .status(LeaveRequest.LeaveStatus.PENDING)
                .build();
        leaveRequestRepository.save(leaveRequest);

        auditService.logAction("LEAVE_APPLIED", getCurrentUserEmail(), null, "LEAVE", leaveRequest.getId(),
                "Leave applied from " + leaveRequest.getFromDate() + " to " + leaveRequest.getToDate());
        return ApiResponse.success("Leave applied successfully", null);
    }

    public ApiResponse<List<LeaveRequestDto>> getAllLeaves() {
        List<LeaveRequest> leaves = leaveRequestRepository.findAllOrderByAppliedAtDesc();
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
        List<LeaveRequest> leaves = leaveRequestRepository.findByStatusOrderByAppliedAtDesc(LeaveRequest.LeaveStatus.PENDING);
        List<LeaveRequestDto> dtos = leaves.stream()
                .map(LeaveRequestDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Map<String, Long>> getLeaveCountByStatus() {
        Map<String, Long> counts = new HashMap<>();
        long pending = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.PENDING);
        long approved = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.APPROVED);
        long rejected = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.REJECTED);
        counts.put("pending", pending);
        counts.put("approved", approved);
        counts.put("rejected", rejected);
        counts.put("total", pending + approved + rejected);

        return ApiResponse.success(counts);
    }
}
