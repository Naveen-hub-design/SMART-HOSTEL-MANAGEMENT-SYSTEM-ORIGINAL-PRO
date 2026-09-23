package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.LeaveRequestDto;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Student;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.DuplicateResourceException;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB-free validation tests for leave application rules.
 * Run with: mvn test -Dtest=LeaveValidationTest
 */
@ExtendWith(MockitoExtension.class)
class LeaveValidationTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WardenRepository wardenRepository;

    @InjectMocks
    private LeaveService leaveService;

    private static final Student STUDENT =
            Student.builder().id(7L).enrollmentNo("ENR007").build();

    private static LeaveRequest existing(String from, String to,
                                         LeaveRequest.LeaveStatus status) {
        return LeaveRequest.builder()
                .id(99L).student(STUDENT)
                .fromDate(from).toDate(to).reason("old")
                .status(status).build();
    }

    private void setupStudent(LeaveRequest... existingLeaves) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "s@hostel.com", null));
        lenient().when(studentRepository.findByUserId(7L))
                .thenReturn(java.util.Optional.of(STUDENT));
        lenient().when(leaveRequestRepository.findByStudentId(7L))
                .thenReturn(List.of(existingLeaves));
    }

    private static LeaveRequestDto dto(String from, String to) {
        return LeaveRequestDto.builder()
                .fromDate(from).toDate(to).reason("trip").build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsBlankDates() {
        setupStudent();
        assertThrows(BadRequestException.class,
                () -> leaveService.applyLeave(7L, dto(null, "2026-09-05")));
        assertThrows(BadRequestException.class,
                () -> leaveService.applyLeave(7L, dto("2026-09-01", "  ")));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void rejectsMalformedDates() {
        setupStudent();
        assertThrows(BadRequestException.class,
                () -> leaveService.applyLeave(7L, dto("09/01/2026", "2026-09-05")));
        assertThrows(BadRequestException.class,
                () -> leaveService.applyLeave(7L, dto("2026-13-01", "2026-09-05")));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void rejectsFromAfterTo() {
        setupStudent();
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> leaveService.applyLeave(7L, dto("2026-09-10", "2026-09-05")));
        assertTrue(ex.getMessage().contains("on or before"));
    }

    @Test
    void rejectsOverlapWithPendingLeave() {
        setupStudent(existing("2026-09-03", "2026-09-07",
                LeaveRequest.LeaveStatus.PENDING));
        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> leaveService.applyLeave(7L, dto("2026-09-05", "2026-09-09")));
        assertTrue(ex.getMessage().contains("overlaps"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void rejectsOverlapWithApprovedLeave() {
        setupStudent(existing("2026-09-01", "2026-09-10",
                LeaveRequest.LeaveStatus.APPROVED));
        assertThrows(DuplicateResourceException.class,
                () -> leaveService.applyLeave(7L, dto("2026-09-10", "2026-09-12")));
    }

    @Test
    void allowsAdjacentNonOverlappingLeave() {
        setupStudent(existing("2026-09-01", "2026-09-05",
                LeaveRequest.LeaveStatus.APPROVED));
        ApiResponse<Void> res =
                leaveService.applyLeave(7L, dto("2026-09-06", "2026-09-08"));
        assertTrue(res.isSuccess());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void ignoresRejectedLeavesForOverlap() {
        setupStudent(existing("2026-09-01", "2026-09-10",
                LeaveRequest.LeaveStatus.REJECTED));
        ApiResponse<Void> res =
                leaveService.applyLeave(7L, dto("2026-09-05", "2026-09-07"));
        assertTrue(res.isSuccess());
    }

    @Test
    void allowsFirstLeave() {
        setupStudent();
        ApiResponse<Void> res =
                leaveService.applyLeave(7L, dto("2026-09-05", "2026-09-07"));
        assertTrue(res.isSuccess());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }
}
