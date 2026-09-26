package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.AttendanceBulkRequest;
import com.hostel.dto.AttendanceBulkResultDto;
import com.hostel.dto.AttendanceCorrectionRequest;
import com.hostel.dto.AttendanceDto;
import com.hostel.dto.AttendanceMarkRequest;
import com.hostel.dto.PageResponse;
import com.hostel.entity.AttendanceRecord;
import com.hostel.entity.AttendanceStatus;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.DuplicateResourceException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.AttendanceRepository;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Attendance marking, listing and correction.
 *
 * Authorization follows the established two-layer pattern: controllers
 * gate roles, and every operation here re-verifies ownership server-side
 * against the warden's assigned block. STUDENT callers may only read
 * their own records. No block or student identifiers from clients are
 * ever trusted for authorization.
 */
@Service
@Transactional
public class AttendanceService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final WardenRepository wardenRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AuditService auditService;

    @Autowired
    @Lazy
    private AttendanceService self;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             StudentRepository studentRepository,
                             UserRepository userRepository,
                             WardenRepository wardenRepository,
                             LeaveRequestRepository leaveRequestRepository,
                             AuditService auditService) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.wardenRepository = wardenRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.auditService = auditService;
    }

    private User getCurrentUser() {
        String currentUserEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));
    }

    private Warden resolveWardenBlock(User currentUser) {
        Warden warden = wardenRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Warden not found for userId: " + currentUser.getId()));
        if (warden.getBlock() == null) {
            throw new AccessDeniedException(
                    "Warden is not assigned to a hostel block");
        }
        return warden;
    }

    /**
     * Verifies the caller may act on the given student. ADMIN passes
     * through; WARDEN must own the student's block; anyone else is denied.
     * Roomless students belong to no block, so wardens can never reach them.
     */
    private void verifyStudentBlockAccess(User currentUser, Student student) {
        if (currentUser.getRole() == User.Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == User.Role.WARDEN) {
            Warden warden = resolveWardenBlock(currentUser);
            HostelBlock wardenBlock = warden.getBlock();
            if (student.getRoom() != null
                    && student.getRoom().getBlock() != null
                    && wardenBlock.getId().equals(
                            student.getRoom().getBlock().getId())) {
                return;
            }
            throw new AccessDeniedException(
                    "You are not authorized to manage attendance for this student");
        }
        throw new AccessDeniedException(
                "You are not authorized to manage attendance");
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
     * Determines the effective status for a fresh mark. An APPROVED leave
     * covering the date forces EXCUSED so normal marks cannot silently
     * override authorized absence. History is stored, never recomputed,
     * so later leave changes cannot rewrite it.
     */
    private AttendanceStatus resolveEffectiveStatus(Student student,
                                                    LocalDate date,
                                                    AttendanceStatus requested) {
        for (LeaveRequest leave :
                leaveRequestRepository.findByStudentId(student.getId())) {
            if (leave.getStatus() != LeaveRequest.LeaveStatus.APPROVED) {
                continue;
            }
            LocalDate from = parseLeaveDate(leave.getFromDate());
            LocalDate to = parseLeaveDate(leave.getToDate());
            if (from == null || to == null) {
                continue;
            }
            if (!date.isBefore(from) && !date.isAfter(to)) {
                return AttendanceStatus.EXCUSED;
            }
        }
        return requested;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<AttendanceDto> markAttendance(AttendanceMarkRequest request) {
        User currentUser = getCurrentUser();

        if (request.getStudentId() == null) {
            throw new BadRequestException("Student ID is required");
        }
        if (request.getDate() == null) {
            throw new BadRequestException("Date is required");
        }
        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student", request.getStudentId()));

        verifyStudentBlockAccess(currentUser, student);

        if (attendanceRepository.existsByStudentIdAndDate(
                student.getId(), request.getDate())) {
            throw new DuplicateResourceException(
                    "Attendance already marked for this student on "
                            + request.getDate());
        }

        AttendanceStatus effectiveStatus = resolveEffectiveStatus(
                student, request.getDate(), request.getStatus());

        AttendanceRecord record = AttendanceRecord.builder()
                .student(student)
                .date(request.getDate())
                .status(effectiveStatus)
                .markedBy(currentUser)
                .markedAt(LocalDateTime.now())
                .remarks(request.getRemarks())
                .build();

        try {
            record = attendanceRepository.save(record);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateResourceException(
                    "Attendance already marked for this student on "
                            + request.getDate());
        }

        auditService.logAction("ATTENDANCE_MARKED",
                getCurrentUserEmail(), currentUser.getRole().name(),
                "ATTENDANCE", record.getId(),
                "Attendance marked " + effectiveStatus + " for student "
                        + student.getId() + " on " + request.getDate());
        return ApiResponse.success(
                "Attendance marked successfully", mapToDto(record));
    }

    /**
     * Bulk marking with per-row isolation (same convention as bulk student
     * import): each row commits independently so one bad row cannot roll
     * back the rest. Authorization is resolved separately for every
     * student; cross-block rows are rejected without leaking details.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApiResponse<AttendanceBulkResultDto> bulkMarkAttendance(
            AttendanceBulkRequest request) {
        if (request.getDate() == null) {
            throw new BadRequestException("Date is required");
        }
        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new BadRequestException(
                    "At least one attendance entry is required");
        }

        List<AttendanceBulkResultDto.RowResult> results = new ArrayList<>();
        int success = 0;
        int rowNumber = 0;

        for (AttendanceBulkRequest.AttendanceBulkEntry entry :
                request.getEntries()) {
            rowNumber++;
            try {
                AttendanceDto dto = self.markSingleRow(
                        request.getDate(), entry);
                success++;
                results.add(AttendanceBulkResultDto.RowResult.builder()
                        .rowNumber(rowNumber)
                        .studentId(dto.getStudentId())
                        .studentName(dto.getStudentName())
                        .status("SUCCESS")
                        .room(dto.getRoomNo())
                        .message("Attendance marked successfully")
                        .build());
            } catch (AccessDeniedException e) {
                results.add(AttendanceBulkResultDto.RowResult.builder()
                        .rowNumber(rowNumber)
                        .studentId(entry.getStudentId())
                        .status("FAILED")
                        .message("You are not authorized to mark attendance "
                                + "for this student")
                        .build());
            } catch (ResourceNotFoundException | BadRequestException
                    | DuplicateResourceException e) {
                results.add(failedRow(rowNumber, entry, e.getMessage()));
            } catch (DataIntegrityViolationException e) {
                results.add(failedRow(rowNumber, entry,
                        "Attendance already marked for this student on "
                                + request.getDate()));
            } catch (Exception e) {
                results.add(failedRow(rowNumber, entry,
                        "Unexpected error while marking attendance"));
            }
        }

        AttendanceBulkResultDto result = AttendanceBulkResultDto.builder()
                .totalRows(request.getEntries().size())
                .successCount(success)
                .failureCount(request.getEntries().size() - success)
                .results(results)
                .build();
        return ApiResponse.success("Bulk attendance completed", result);
    }

    private AttendanceBulkResultDto.RowResult failedRow(int rowNumber,
            AttendanceBulkRequest.AttendanceBulkEntry entry, String message) {
        String name = null;
        String room = null;
        try {
            Student student = studentRepository.findById(entry.getStudentId())
                    .orElse(null);
            if (student != null && student.getUser() != null) {
                name = student.getUser().getName();
                if (student.getRoom() != null) {
                    room = student.getRoom().getRoomNo();
                }
            }
        } catch (Exception ignored) {
            // Best-effort enrichment only; never fail the row over it.
        }
        return AttendanceBulkResultDto.RowResult.builder()
                .rowNumber(rowNumber)
                .studentId(entry.getStudentId())
                .studentName(name)
                .status("FAILED")
                .room(room)
                .message(message)
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttendanceDto markSingleRow(LocalDate date,
            AttendanceBulkRequest.AttendanceBulkEntry entry) {
        if (entry.getStudentId() == null) {
            throw new BadRequestException("Student ID is required");
        }
        if (entry.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }
        AttendanceMarkRequest single = AttendanceMarkRequest.builder()
                .studentId(entry.getStudentId())
                .date(date)
                .status(entry.getStatus())
                .remarks(entry.getRemarks())
                .build();
        return self.markAttendance(single).getData();
    }

    public ApiResponse<PageResponse<AttendanceDto>> searchAttendance(
            LocalDate date, AttendanceStatus status,
            Integer page, Integer size) {
        User currentUser = getCurrentUser();

        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<AttendanceRecord> records;
        if (currentUser.getRole() == User.Role.WARDEN) {
            Warden warden = resolveWardenBlock(currentUser);
            records = attendanceRepository.searchByBlock(
                    warden.getBlock().getId(), date, status, pageable);
        } else if (currentUser.getRole() == User.Role.ADMIN) {
            records = attendanceRepository.searchAll(date, status, pageable);
        } else {
            throw new AccessDeniedException(
                    "You are not authorized to view attendance");
        }

        return ApiResponse.success(
                PageResponse.fromPage(records, this::mapToDto));
    }

    public ApiResponse<PageResponse<AttendanceDto>> getMyAttendance(
            LocalDate date, AttendanceStatus status,
            Integer page, Integer size) {
        User currentUser = getCurrentUser();
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found for userId: "
                                + currentUser.getId()));

        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);

        Page<AttendanceRecord> records =
                attendanceRepository.searchByStudent(student.getId(), date,
                        status, PageRequest.of(safePage, safeSize));

        return ApiResponse.success(
                PageResponse.fromPage(records, this::mapToDto));
    }

    /**
     * Explicit correction flow: status and remarks only (date is
     * immutable, so uniqueness can never collide). The correction is
     * recorded with the correcting user and timestamp.
     */
    public ApiResponse<AttendanceDto> correctAttendance(Long id,
            AttendanceCorrectionRequest request) {
        User currentUser = getCurrentUser();

        if (request.getStatus() == null) {
            throw new BadRequestException("Status is required");
        }

        AttendanceRecord record = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AttendanceRecord", id));

        verifyStudentBlockAccess(currentUser, record.getStudent());

        record.setStatus(request.getStatus());
        record.setRemarks(request.getRemarks());
        record.setMarkedBy(currentUser);
        record.setMarkedAt(LocalDateTime.now());
        record = attendanceRepository.save(record);

        auditService.logAction("ATTENDANCE_CORRECTED",
                getCurrentUserEmail(), currentUser.getRole().name(),
                "ATTENDANCE", record.getId(),
                "Attendance corrected to " + request.getStatus()
                        + " for record " + record.getId());
        return ApiResponse.success(
                "Attendance corrected successfully", mapToDto(record));
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private AttendanceDto mapToDto(AttendanceRecord record) {
        Student student = record.getStudent();
        User studentUser = student != null ? student.getUser() : null;
        return AttendanceDto.builder()
                .id(record.getId())
                .studentId(student != null ? student.getId() : null)
                .studentName(studentUser != null
                        ? studentUser.getName() : null)
                .enrollmentNo(student != null
                        ? student.getEnrollmentNo() : null)
                .roomNo(student != null && student.getRoom() != null
                        ? student.getRoom().getRoomNo() : null)
                .blockName(student != null && student.getRoom() != null
                        && student.getRoom().getBlock() != null
                        ? student.getRoom().getBlock().getName() : null)
                .date(record.getDate())
                .status(record.getStatus() != null
                        ? record.getStatus().name() : null)
                .remarks(record.getRemarks())
                .markedByName(record.getMarkedBy() != null
                        ? record.getMarkedBy().getName() : null)
                .markedAt(record.getMarkedAt())
                .build();
    }
}
