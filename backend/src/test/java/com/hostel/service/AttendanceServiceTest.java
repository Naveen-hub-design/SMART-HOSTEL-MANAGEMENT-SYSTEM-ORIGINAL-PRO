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
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.DuplicateResourceException;
import com.hostel.repository.AttendanceRepository;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DB-free tests for attendance authorization, validation, duplicates,
 * leave integration and audit. Run with:
 * mvn test -Dtest=AttendanceServiceTest
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WardenRepository wardenRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AttendanceService attendanceService;

    private static final LocalDate DAY = LocalDate.of(2026, 9, 24);

    private static final HostelBlock BLOCK_A =
            HostelBlock.builder().id(1L).name("A Wing").code("A").build();
    private static final HostelBlock BLOCK_B =
            HostelBlock.builder().id(2L).name("B Wing").code("B").build();

    private static final User ADMIN =
            User.builder().id(1L).name("Admin").email("a@hostel.com")
                    .password("h").role(User.Role.ADMIN).build();
    private static final User WARDEN_USER =
            User.builder().id(2L).name("Warden").email("w@hostel.com")
                    .password("h").role(User.Role.WARDEN).build();
    private static final User STUDENT_USER =
            User.builder().id(4L).name("Stu").email("s@hostel.com")
                    .password("h").role(User.Role.STUDENT).build();

    private static final Warden WARDEN =
            Warden.builder().id(9L).user(WARDEN_USER).block(BLOCK_A)
                    .qualification("M.Sc").build();

    private static Room room(Long id, HostelBlock block) {
        return Room.builder().id(id).roomNo("R-" + id).block(block)
                .floor(1).capacity(2).occupants(1)
                .status(Room.RoomStatus.AVAILABLE).build();
    }

    private static Student student(Long id, HostelBlock block) {
        User u = User.builder().id(100L + id).name("Stu" + id)
                .email("s" + id + "@hostel.com").password("h")
                .role(User.Role.STUDENT).build();
        return Student.builder().id(id).user(u)
                .room(block != null ? room(50L + id, block) : null)
                .enrollmentNo("ENR" + id).build();
    }

    private static LeaveRequest leave(LeaveRequest.LeaveStatus status,
                                      String from, String to) {
        return LeaveRequest.builder().id(60L).student(student(7L, BLOCK_A))
                .fromDate(from).toDate(to).reason("trip")
                .status(status).build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null));
        lenient().when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
    }

    private void stubWardenBlock() {
        lenient().when(wardenRepository.findByUserId(WARDEN_USER.getId()))
                .thenReturn(Optional.of(WARDEN));
    }

    private static AttendanceMarkRequest mark(Long studentId,
                                              AttendanceStatus status) {
        return AttendanceMarkRequest.builder().studentId(studentId)
                .date(DAY).status(status).remarks("muster").build();
    }

    @BeforeEach
    void wireSelfProxy() {
        // Bulk paths call through the lazy self-proxy; in unit tests the
        // real instance plays that role (no transactions involved).
        ReflectionTestUtils.setField(attendanceService, "self",
                attendanceService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void wardenMarksOwnBlockStudent() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        Student s = student(7L, BLOCK_A);
        when(studentRepository.findById(7L)).thenReturn(Optional.of(s));
        when(attendanceRepository.existsByStudentIdAndDate(7L, DAY))
                .thenReturn(false);
        when(leaveRequestRepository.findByStudentId(7L))
                .thenReturn(List.of());
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        ApiResponse<AttendanceDto> res =
                attendanceService.markAttendance(mark(7L, AttendanceStatus.PRESENT));

        assertTrue(res.isSuccess());
        assertEquals(AttendanceStatus.PRESENT.name(),
                res.getData().getStatus());
        verify(auditService).logAction(eq("ATTENDANCE_MARKED"), any(),
                any(), eq("ATTENDANCE"), any(), any());
    }

    @Test
    void wardenCannotMarkOtherBlockStudent() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(17L))
                .thenReturn(Optional.of(student(17L, BLOCK_B)));

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.markAttendance(
                        mark(17L, AttendanceStatus.PRESENT)));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void wardenEditsOwnBlockRecord() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        Student s = student(7L, BLOCK_A);
        AttendanceRecord record = AttendanceRecord.builder().id(3L)
                .student(s).date(DAY).status(AttendanceStatus.ABSENT).build();
        when(attendanceRepository.findById(3L))
                .thenReturn(Optional.of(record));
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        AttendanceCorrectionRequest correction =
                AttendanceCorrectionRequest.builder()
                        .status(AttendanceStatus.LATE).remarks("late bus")
                        .build();
        ApiResponse<AttendanceDto> res =
                attendanceService.correctAttendance(3L, correction);

        assertTrue(res.isSuccess());
        assertEquals("LATE", res.getData().getStatus());
        assertEquals("late bus", res.getData().getRemarks());
        assertEquals(WARDEN_USER.getId(),
                record.getMarkedBy().getId());
        assertNotNull(record.getMarkedAt());
    }

    @Test
    void wardenCannotEditOtherBlockRecord() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        AttendanceRecord record = AttendanceRecord.builder().id(4L)
                .student(student(17L, BLOCK_B)).date(DAY)
                .status(AttendanceStatus.ABSENT).build();
        when(attendanceRepository.findById(4L))
                .thenReturn(Optional.of(record));

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.correctAttendance(4L,
                        AttendanceCorrectionRequest.builder()
                                .status(AttendanceStatus.PRESENT).build()));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminMarksGlobally() {
        authenticateAs(ADMIN);
        Student s = student(17L, BLOCK_B);
        when(studentRepository.findById(17L)).thenReturn(Optional.of(s));
        when(attendanceRepository.existsByStudentIdAndDate(17L, DAY))
                .thenReturn(false);
        when(leaveRequestRepository.findByStudentId(17L))
                .thenReturn(List.of());
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertTrue(attendanceService
                .markAttendance(mark(17L, AttendanceStatus.PRESENT))
                .isSuccess());
    }

    @Test
    void adminEditsGlobally() {
        authenticateAs(ADMIN);
        AttendanceRecord record = AttendanceRecord.builder().id(5L)
                .student(student(17L, BLOCK_B)).date(DAY)
                .status(AttendanceStatus.ABSENT).build();
        when(attendanceRepository.findById(5L))
                .thenReturn(Optional.of(record));
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertTrue(attendanceService.correctAttendance(5L,
                AttendanceCorrectionRequest.builder()
                        .status(AttendanceStatus.PRESENT).build())
                .isSuccess());
    }

    @Test
    void studentCannotMark() {
        authenticateAs(STUDENT_USER);
        when(studentRepository.findById(7L))
                .thenReturn(Optional.of(student(7L, BLOCK_A)));

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.markAttendance(
                        mark(7L, AttendanceStatus.PRESENT)));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void studentCannotEdit() {
        authenticateAs(STUDENT_USER);
        AttendanceRecord record = AttendanceRecord.builder().id(3L)
                .student(student(7L, BLOCK_A)).date(DAY)
                .status(AttendanceStatus.ABSENT).build();
        when(attendanceRepository.findById(3L))
                .thenReturn(Optional.of(record));

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.correctAttendance(3L,
                        AttendanceCorrectionRequest.builder()
                                .status(AttendanceStatus.PRESENT).build()));
    }

    @Test
    void studentRetrievesOwnAttendance() {
        authenticateAs(STUDENT_USER);
        Student s = student(7L, BLOCK_A);
        when(studentRepository.findByUserId(STUDENT_USER.getId()))
                .thenReturn(Optional.of(s));
        AttendanceRecord record = AttendanceRecord.builder().id(6L)
                .student(s).date(DAY).status(AttendanceStatus.PRESENT).build();
        when(attendanceRepository.searchByStudent(eq(7L), eq(null),
                eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        var res = attendanceService.getMyAttendance(null, null, 0, 10);

        assertTrue(res.isSuccess());
        assertEquals(1, res.getData().getTotalElements());
        verify(studentRepository).findByUserId(STUDENT_USER.getId());
    }

    @Test
    void duplicateReturns409() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(7L))
                .thenReturn(Optional.of(student(7L, BLOCK_A)));
        when(attendanceRepository.existsByStudentIdAndDate(7L, DAY))
                .thenReturn(true);

        assertThrows(
                com.hostel.exception.DuplicateResourceException.class,
                () -> attendanceService.markAttendance(
                        mark(7L, AttendanceStatus.PRESENT)));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void approvedLeaveProducesExcused() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(7L))
                .thenReturn(Optional.of(student(7L, BLOCK_A)));
        when(attendanceRepository.existsByStudentIdAndDate(7L, DAY))
                .thenReturn(false);
        when(leaveRequestRepository.findByStudentId(7L)).thenReturn(List.of(
                leave(LeaveRequest.LeaveStatus.APPROVED,
                        "2026-09-20", "2026-09-26")));
        ArgumentCaptor<AttendanceRecord> captor =
                ArgumentCaptor.forClass(AttendanceRecord.class);
        when(attendanceRepository.save(captor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        ApiResponse<AttendanceDto> res = attendanceService
                .markAttendance(mark(7L, AttendanceStatus.PRESENT));

        assertEquals("EXCUSED", res.getData().getStatus());
        assertEquals(AttendanceStatus.EXCUSED, captor.getValue().getStatus());
    }

    @Test
    void pendingLeaveDoesNotProduceExcused() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(7L))
                .thenReturn(Optional.of(student(7L, BLOCK_A)));
        when(attendanceRepository.existsByStudentIdAndDate(7L, DAY))
                .thenReturn(false);
        when(leaveRequestRepository.findByStudentId(7L)).thenReturn(List.of(
                leave(LeaveRequest.LeaveStatus.PENDING,
                        "2026-09-20", "2026-09-26")));
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertEquals("PRESENT", attendanceService
                .markAttendance(mark(7L, AttendanceStatus.PRESENT))
                .getData().getStatus());
    }

    @Test
    void rejectedLeaveDoesNotProduceExcused() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(7L))
                .thenReturn(Optional.of(student(7L, BLOCK_A)));
        when(attendanceRepository.existsByStudentIdAndDate(7L, DAY))
                .thenReturn(false);
        when(leaveRequestRepository.findByStudentId(7L)).thenReturn(List.of(
                leave(LeaveRequest.LeaveStatus.REJECTED,
                        "2026-09-20", "2026-09-26")));
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertEquals("ABSENT", attendanceService
                .markAttendance(mark(7L, AttendanceStatus.ABSENT))
                .getData().getStatus());
    }

    @Test
    void historyStableOnRead() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        AttendanceRecord record = AttendanceRecord.builder().id(8L)
                .student(student(7L, BLOCK_A)).date(DAY)
                .status(AttendanceStatus.EXCUSED).build();
        when(attendanceRepository.searchByBlock(eq(1L), eq(null), eq(null),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        var res = attendanceService.searchAttendance(null, null, 0, 10);

        assertEquals("EXCUSED", res.getData().getContent().get(0).getStatus());
        verify(leaveRequestRepository, never()).findByStudentId(any());
    }

    @Test
    void roomlessStudentHiddenFromWarden() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(50L))
                .thenReturn(Optional.of(student(50L, null)));

        assertThrows(AccessDeniedException.class,
                () -> attendanceService.markAttendance(
                        mark(50L, AttendanceStatus.PRESENT)));
    }

    @Test
    void roomlessStudentAllowedForAdmin() {
        authenticateAs(ADMIN);
        when(studentRepository.findById(50L))
                .thenReturn(Optional.of(student(50L, null)));
        when(attendanceRepository.existsByStudentIdAndDate(50L, DAY))
                .thenReturn(false);
        when(leaveRequestRepository.findByStudentId(50L))
                .thenReturn(List.of());
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertTrue(attendanceService
                .markAttendance(mark(50L, AttendanceStatus.PRESENT))
                .isSuccess());
    }

    @Test
    void invalidInputRejected() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();

        assertThrows(BadRequestException.class,
                () -> attendanceService.markAttendance(
                        AttendanceMarkRequest.builder().studentId(null)
                                .date(DAY)
                                .status(AttendanceStatus.PRESENT).build()));
        assertThrows(BadRequestException.class,
                () -> attendanceService.markAttendance(
                        AttendanceMarkRequest.builder().studentId(7L)
                                .date(null)
                                .status(AttendanceStatus.PRESENT).build()));
        assertThrows(BadRequestException.class,
                () -> attendanceService.markAttendance(
                        AttendanceMarkRequest.builder().studentId(7L)
                                .date(DAY).status(null).build()));
    }

    @Test
    void missingStudentReturns404() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                com.hostel.exception.ResourceNotFoundException.class,
                () -> attendanceService.markAttendance(
                        mark(999L, AttendanceStatus.PRESENT)));
    }

    @Test
    void bulkIsolatesCrossBlockRows() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.findById(7L))
                .thenReturn(Optional.of(student(7L, BLOCK_A)));
        when(studentRepository.findById(17L))
                .thenReturn(Optional.of(student(17L, BLOCK_B)));
        when(attendanceRepository.existsByStudentIdAndDate(eq(7L), eq(DAY)))
                .thenReturn(false);
        when(leaveRequestRepository.findByStudentId(7L))
                .thenReturn(List.of());
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        AttendanceBulkRequest bulk = AttendanceBulkRequest.builder()
                .date(DAY)
                .entries(List.of(
                        AttendanceBulkRequest.AttendanceBulkEntry.builder()
                                .studentId(7L)
                                .status(AttendanceStatus.PRESENT).build(),
                        AttendanceBulkRequest.AttendanceBulkEntry.builder()
                                .studentId(17L)
                                .status(AttendanceStatus.PRESENT).build()))
                .build();

        var result = attendanceService.bulkMarkAttendance(bulk).getData();

        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("SUCCESS", result.getResults().get(0).getStatus());
        assertEquals("Stu7", result.getResults().get(0).getStudentName());
        assertEquals("FAILED", result.getResults().get(1).getStatus());
        assertNull(result.getResults().get(1).getStudentName(),
                "Cross-block row must not leak student details");
        assertNull(result.getResults().get(1).getRoom());
    }
}
