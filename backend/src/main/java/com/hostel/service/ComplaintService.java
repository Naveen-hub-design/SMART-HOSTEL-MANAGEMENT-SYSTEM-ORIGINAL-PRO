package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.ComplaintDto;
import com.hostel.entity.Complaint;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.ComplaintRepository;
import com.hostel.repository.StudentRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;
    private final FileUploadService fileUploadService;
    private final EmailService emailService;
    private final AuditService auditService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            StudentRepository studentRepository,
                            FileUploadService fileUploadService,
                            EmailService emailService,
                            AuditService auditService) {
        this.complaintRepository = complaintRepository;
        this.studentRepository = studentRepository;
        this.fileUploadService = fileUploadService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public ApiResponse<Void> createComplaint(Long userId, ComplaintDto complaintDto, MultipartFile image) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileUploadService.uploadFile(image);
        }

        Complaint.ComplaintCategory category = Complaint.ComplaintCategory.GENERAL;
        if (complaintDto.getCategory() != null) {
            try {
                category = Complaint.ComplaintCategory.valueOf(complaintDto.getCategory().toUpperCase());
            } catch (IllegalArgumentException e) {
                category = Complaint.ComplaintCategory.GENERAL;
            }
        }

        Complaint complaint = Complaint.builder()
                .student(student)
                .title(complaintDto.getTitle())
                .description(complaintDto.getDescription())
                .imageUrl(imageUrl)
                .category(category)
                .status(Complaint.ComplaintStatus.PENDING)
                .build();
        complaintRepository.save(complaint);

        auditService.logAction("COMPLAINT_SUBMITTED", getCurrentUserEmail(), null, "COMPLAINT", complaint.getId(),
                "Complaint submitted: " + complaint.getTitle());
        return ApiResponse.success("Complaint submitted successfully", null);
    }

    public ApiResponse<List<ComplaintDto>> getAllComplaints() {
        List<Complaint> complaints = complaintRepository.findAllOrderByCreatedAtDesc();
        List<ComplaintDto> dtos = complaints.stream()
                .map(ComplaintDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<ComplaintDto>> getStudentComplaints(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        List<Complaint> complaints = complaintRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<ComplaintDto> dtos = complaints.stream()
                .map(ComplaintDto::fromEntity)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Void> updateComplaintStatus(Long complaintId, String status) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", complaintId));

        Complaint.ComplaintStatus oldStatus = complaint.getStatus();
        Complaint.ComplaintStatus newStatus = Complaint.ComplaintStatus.valueOf(status.toUpperCase());
        complaint.setStatus(newStatus);
        if (newStatus == Complaint.ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        complaintRepository.save(complaint);

        emailService.sendComplaintUpdate(
                complaint.getStudent().getUser().getEmail(),
                complaint.getStudent().getUser().getName(),
                complaint.getTitle(),
                newStatus.name()
        );

        auditService.logAction("COMPLAINT_STATUS_UPDATED", getCurrentUserEmail(), null, "COMPLAINT", complaint.getId(),
                "Complaint status updated from " + oldStatus + " to " + newStatus);
        return ApiResponse.success("Complaint status updated successfully", null);
    }

    public ApiResponse<Map<String, Long>> getComplaintCountByStatus() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("PENDING", complaintRepository.countByStatus(Complaint.ComplaintStatus.PENDING));
        counts.put("IN_PROGRESS", complaintRepository.countByStatus(Complaint.ComplaintStatus.IN_PROGRESS));
        counts.put("RESOLVED", complaintRepository.countByStatus(Complaint.ComplaintStatus.RESOLVED));
        counts.put("REJECTED", complaintRepository.countByStatus(Complaint.ComplaintStatus.REJECTED));

        return ApiResponse.success(counts);
    }
}
