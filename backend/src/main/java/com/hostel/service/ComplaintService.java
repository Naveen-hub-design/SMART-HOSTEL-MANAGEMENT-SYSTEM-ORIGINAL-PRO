package com.hostel.service;

import com.hostel.dto.AICategorizationResponse;
import com.hostel.dto.ApiResponse;
import com.hostel.dto.ComplaintDto;
import com.hostel.entity.Complaint;
import com.hostel.entity.MessFeedback;
import com.hostel.entity.Student;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.ComplaintRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.Warden;
import com.hostel.repository.WardenRepository;
import com.hostel.entity.User;
import com.hostel.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;
    private final FileUploadService fileUploadService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final AIService aiService;
    private final WardenRepository wardenRepository;
    private final UserRepository userRepository;

    public ComplaintService(ComplaintRepository complaintRepository,
                            StudentRepository studentRepository,
                            FileUploadService fileUploadService,
                            EmailService emailService,
                            AuditService auditService,
                            AIService aiService,
                            WardenRepository wardenRepository,
                            UserRepository userRepository) {
        this.complaintRepository = complaintRepository;
        this.studentRepository = studentRepository;
        this.fileUploadService = fileUploadService;
        this.emailService = emailService;
        this.auditService = auditService;
        this.aiService = aiService;
        this.wardenRepository = wardenRepository;
        this.userRepository = userRepository;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private void verifyWardenBlockAccess(Long wardenUserId, Complaint complaint) {

        Warden warden = wardenRepository.findByUserId(wardenUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Warden not found for userId: " + wardenUserId));

        HostelBlock wardenBlock = warden.getBlock();

        if (wardenBlock == null) {
            throw new AccessDeniedException(
                    "Warden is not assigned to a hostel block");
        }

        Student student = complaint.getStudent();

        if (student == null ||
                student.getRoom() == null ||
                student.getRoom().getBlock() == null) {

            throw new AccessDeniedException(
                    "Student is not assigned to a hostel block");
        }

        Long studentBlockId = student.getRoom().getBlock().getId();

        if (!wardenBlock.getId().equals(studentBlockId)) {
            throw new AccessDeniedException(
                    "You are not authorized to access this complaint");
        }
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

        ComplaintAnalysis analysis = analyzeComplaint(complaintDto.getTitle(), complaintDto.getDescription());

        Complaint complaint = Complaint.builder()
                .student(student)
                .title(complaintDto.getTitle())
                .description(complaintDto.getDescription())
                .imageUrl(imageUrl)
                .category(category)
                .status(Complaint.ComplaintStatus.PENDING)
                .aiCategory(analysis.aiCategory)
                .aiConfidence(analysis.aiConfidence)
                .sentiment(analysis.sentiment)
                .priority(analysis.priority)
                .aiRecommendation(analysis.recommendation)
                .build();
        complaintRepository.save(complaint);

        auditService.logAction("COMPLAINT_SUBMITTED", getCurrentUserEmail(), null, "COMPLAINT", complaint.getId(),
                "Complaint submitted: " + complaint.getTitle()
                        + " (AI: category=" + analysis.aiCategory
                        + ", priority=" + analysis.priority
                        + ", sentiment=" + analysis.sentiment + ")");
        return ApiResponse.success("Complaint submitted successfully", null);
    }

    private record ComplaintAnalysis(String aiCategory, Double aiConfidence,
                                     MessFeedback.Sentiment sentiment,
                                     Complaint.ComplaintPriority priority,
                                     String recommendation) {
    }

    /**
     * Runs the existing AI pipeline (categorization, sentiment, priority, recommendation).
     *
     * <p>Never throws: if any AI step fails the complaint is still created with safe defaults.</p>
     */
    private ComplaintAnalysis analyzeComplaint(String title, String description) {
        try {
            AICategorizationResponse categorization = aiService.categorizeComplaint(title, description);
            String aiCategory = categorization.getCategory();
            Double aiConfidence = categorization.getConfidenceScore() != null
                    ? categorization.getConfidenceScore() : 0.0;
            String sentimentText = aiService.analyzeSentiment((title == null ? "" : title)
                    + " " + (description == null ? "" : description));
            MessFeedback.Sentiment sentiment = parseSentiment(sentimentText);
            Complaint.ComplaintPriority priority = aiService.analyzePriority(title, description);
            String recommendation = aiService.generateRecommendation(aiCategory, priority, title, description);
            return new ComplaintAnalysis(aiCategory, aiConfidence, sentiment, priority, recommendation);
        } catch (Exception e) {
            log.warn("AI complaint analysis failed; using safe defaults: {}", e.getMessage());
            return new ComplaintAnalysis(null, 0.0, null,
                    Complaint.ComplaintPriority.MEDIUM, null);
        }
    }

    private MessFeedback.Sentiment parseSentiment(String sentimentText) {
        if (sentimentText == null || sentimentText.isBlank()) {
            return null;
        }
        try {
            return MessFeedback.Sentiment.valueOf(sentimentText.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public ApiResponse<List<ComplaintDto>> getAllComplaints() {

        String currentUserEmail = getCurrentUserEmail();

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));

        List<Complaint> complaints;

        if (currentUser.getRole() == User.Role.WARDEN) {

            Warden warden = wardenRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Warden not found for userId: " + currentUser.getId()));

            if (warden.getBlock() == null) {
                throw new AccessDeniedException(
                        "Warden is not assigned to a hostel block");
            }

            complaints = complaintRepository
                    .findByBlockIdOrderByCreatedAtDesc(
                            warden.getBlock().getId());

        } else if (currentUser.getRole() == User.Role.ADMIN) {

            complaints = complaintRepository.findAllOrderByCreatedAtDesc();

        } else {

            throw new AccessDeniedException(
                    "You are not authorized to view complaints");
        }

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
                String currentUserEmail = getCurrentUserEmail();

                User currentUser = userRepository.findByEmail(currentUserEmail)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with email: " + currentUserEmail));

                if (currentUser.getRole() == User.Role.WARDEN) {
                    verifyWardenBlockAccess(currentUser.getId(), complaint);
                }

        if (status == null || status.isBlank()) {
            throw new BadRequestException("Complaint status is required");
        }

        Complaint.ComplaintStatus newStatus;
        try {
            newStatus = Complaint.ComplaintStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid complaint status: " + status
                    + " (allowed: PENDING, IN_PROGRESS, RESOLVED, REJECTED)");
        }

        Complaint.ComplaintStatus oldStatus = complaint.getStatus();
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

        String currentUserEmail = getCurrentUserEmail();

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));

        Map<String, Long> counts = new HashMap<>();

        long pending;
        long inProgress;
        long resolved;
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

            pending = complaintRepository.countByBlockIdAndStatus(
                    blockId,
                    Complaint.ComplaintStatus.PENDING);

            inProgress = complaintRepository.countByBlockIdAndStatus(
                    blockId,
                    Complaint.ComplaintStatus.IN_PROGRESS);

            resolved = complaintRepository.countByBlockIdAndStatus(
                    blockId,
                    Complaint.ComplaintStatus.RESOLVED);

            rejected = complaintRepository.countByBlockIdAndStatus(
                    blockId,
                    Complaint.ComplaintStatus.REJECTED);

        } else if (currentUser.getRole() == User.Role.ADMIN) {

            pending = complaintRepository.countByStatus(
                    Complaint.ComplaintStatus.PENDING);

            inProgress = complaintRepository.countByStatus(
                    Complaint.ComplaintStatus.IN_PROGRESS);

            resolved = complaintRepository.countByStatus(
                    Complaint.ComplaintStatus.RESOLVED);

            rejected = complaintRepository.countByStatus(
                    Complaint.ComplaintStatus.REJECTED);

        } else {

            throw new AccessDeniedException(
                    "You are not authorized to view complaint statistics");
        }

        counts.put("pending", pending);
        counts.put("inProgress", inProgress);
        counts.put("resolved", resolved);
        counts.put("rejected", rejected);
        counts.put("total", pending + inProgress + resolved + rejected);

        return ApiResponse.success(counts);
    }
}
