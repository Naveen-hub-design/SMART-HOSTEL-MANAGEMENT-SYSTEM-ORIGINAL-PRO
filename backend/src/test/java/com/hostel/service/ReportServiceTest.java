package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.BlockStatsDto;
import com.hostel.dto.DashboardStatsDto;
import com.hostel.entity.Complaint;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.repository.ComplaintRepository;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * DB-free tests for report scoping and file generation.
 * Run with: mvn test -Dtest=ReportServiceTest
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WardenRepository wardenRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private AdminService adminService;

    @Mock
    private WardenService wardenService;

    @InjectMocks
    private ReportService reportService;

    private static final HostelBlock BLOCK =
            HostelBlock.builder().id(5L).name("A Wing - Senior Boys")
                    .code("A-BLOCK").build();

    private static final User ADMIN =
            User.builder().id(1L).name("Admin").email("a@hostel.com")
                    .password("h").role(User.Role.ADMIN).build();

    private static final User WARDEN_USER =
            User.builder().id(2L).name("Warden").email("w@hostel.com")
                    .password("h").role(User.Role.WARDEN).build();

    private static final Warden WARDEN =
            Warden.builder().id(9L).user(WARDEN_USER).block(BLOCK)
                    .qualification("M.Sc").build();

    private static Student student() {
        User u = User.builder().id(7L).name("Stu").email("s@hostel.com")
                .password("h").phone("999").role(User.Role.STUDENT).build();
        Room r = Room.builder().id(3L).roomNo("A-101").block(BLOCK)
                .floor(1).capacity(2).occupants(1)
                .status(Room.RoomStatus.AVAILABLE).rent(5000.0).build();
        return Student.builder().id(7L).user(u).room(r)
                .enrollmentNo("ENR007").gender(Student.Gender.MALE).build();
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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static void assertPdf(byte[] content) {
        assertNotNull(content);
        assertTrue(content.length > 0);
        String header = new String(content, 0,
                Math.min(5, content.length));
        assertEquals("%PDF-", header);
    }

    private static Workbook openWorkbook(byte[] content) throws Exception {
        assertNotNull(content);
        assertTrue(content.length > 0);
        return WorkbookFactory.create(new ByteArrayInputStream(content));
    }

    @Test
    void adminStudentsPdfHasSignature() {
        authenticateAs(ADMIN);
        when(studentRepository.findAll()).thenReturn(List.of(student()));

        ReportService.GeneratedReport report =
                reportService.generateStudentsPdf();

        assertPdf(report.content());
        assertEquals("hostel-students-report.pdf", report.filename());
    }

    @Test
    void adminStudentsXlsxOpens() throws Exception {
        authenticateAs(ADMIN);
        when(studentRepository.findAll()).thenReturn(List.of(student()));

        ReportService.GeneratedReport report =
                reportService.generateStudentsXlsx();

        try (Workbook wb = openWorkbook(report.content())) {
            Sheet sheet = wb.getSheet("Students");
            assertNotNull(sheet);
            assertEquals("Enrollment No",
                    sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("ENR007",
                    sheet.getRow(1).getCell(0).getStringCellValue());
        }
        assertEquals("hostel-students-report.xlsx", report.filename());
    }

    @Test
    void wardenStudentsScopedToOwnBlock() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(studentRepository.searchByBlock(eq(5L), eq(null), eq(null),
                eq(null), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(student())));

        ReportService.GeneratedReport report =
                reportService.generateStudentsPdf();

        assertPdf(report.content());
        assertEquals("a-wing-senior-boys-students-report.pdf",
                report.filename());
        verify(studentRepository, never()).findAll();
    }

    @Test
    void wardenWithoutBlockDenied() {
        authenticateAs(WARDEN_USER);
        Warden unassigned = Warden.builder().id(10L).user(WARDEN_USER)
                .block(null).build();
        when(wardenRepository.findByUserId(WARDEN_USER.getId()))
                .thenReturn(Optional.of(unassigned));

        assertThrows(AccessDeniedException.class,
                () -> reportService.generateStudentsPdf());
    }

    @Test
    void wardenRoomsPdfScoped() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        Room room = student().getRoom();
        when(roomRepository.findByBlockId(5L)).thenReturn(List.of(room));

        ReportService.GeneratedReport report =
                reportService.generateRoomsPdf();

        assertPdf(report.content());
        verify(roomRepository, never()).findAll();
    }

    @Test
    void adminRoomsXlsxOpens() throws Exception {
        authenticateAs(ADMIN);
        when(roomRepository.findAll())
                .thenReturn(List.of(student().getRoom()));

        try (Workbook wb = openWorkbook(
                reportService.generateRoomsXlsx().content())) {
            assertNotNull(wb.getSheet("Rooms"));
        }
    }

    @Test
    void leavesPdfAndXlsx() throws Exception {
        authenticateAs(ADMIN);
        LeaveRequest leave = LeaveRequest.builder().id(3L).student(student())
                .fromDate("2026-09-01").toDate("2026-09-03").reason("trip")
                .status(LeaveRequest.LeaveStatus.APPROVED)
                .appliedAt(LocalDateTime.of(2026, 8, 30, 10, 0)).build();
        when(leaveRequestRepository.findAllOrderByAppliedAtDesc())
                .thenReturn(List.of(leave));

        assertPdf(reportService.generateLeavesPdf().content());
        try (Workbook wb = openWorkbook(
                reportService.generateLeavesXlsx().content())) {
            assertNotNull(wb.getSheet("Leaves"));
        }
    }

    @Test
    void wardenLeavesScoped() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(leaveRequestRepository.findByBlockIdOrderByAppliedAtDesc(5L))
                .thenReturn(List.of());

        assertPdf(reportService.generateLeavesPdf().content());
        verify(leaveRequestRepository, never())
                .findAllOrderByAppliedAtDesc();
    }

    @Test
    void complaintsPdfAndXlsx() throws Exception {
        authenticateAs(ADMIN);
        Complaint complaint = Complaint.builder().id(4L).student(student())
                .title("Fan").description("Broken fan")
                .status(Complaint.ComplaintStatus.PENDING)
                .createdAt(LocalDateTime.of(2026, 8, 29, 9, 0)).build();
        when(complaintRepository.findAllOrderByCreatedAtDesc())
                .thenReturn(List.of(complaint));

        assertPdf(reportService.generateComplaintsPdf().content());
        try (Workbook wb = openWorkbook(
                reportService.generateComplaintsXlsx().content())) {
            assertNotNull(wb.getSheet("Complaints"));
        }
    }

    @Test
    void wardenComplaintsScoped() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        when(complaintRepository.findByBlockIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of());

        assertPdf(reportService.generateComplaintsPdf().content());
        verify(complaintRepository, never())
                .findAllOrderByCreatedAtDesc();
    }

    @Test
    void adminSummaryPdfAndXlsx() throws Exception {
        authenticateAs(ADMIN);
        when(adminService.getReportSummary()).thenReturn(Map.of(
                "totalStudents", 10L, "totalWardens", 2L, "totalRooms", 8L,
                "totalBlocks", 2L, "occupancyRate", 50L, "roomsWithRent", 8L,
                "averageRent", 5000.0, "activeStudents", 9L));
        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalLeaves(5L).pendingLeaves(2L).approvedLeaves(2L)
                .totalComplaints(4L).pendingComplaints(1L)
                .resolvedComplaints(2L)
                .blockStats(List.of(BlockStatsDto.builder().name("A Wing")
                        .capacity(8L).occupied(4L).build()))
                .build();
        when(adminService.getDashboardStats())
                .thenReturn(ApiResponse.success(stats));

        assertPdf(reportService.generateSummaryPdf().content());
        try (Workbook wb = openWorkbook(
                reportService.generateSummaryXlsx().content())) {
            assertNotNull(wb.getSheet("Summary"));
            assertNotNull(wb.getSheet("Blocks"));
        }
    }

    @Test
    void wardenSummaryScoped() {
        authenticateAs(WARDEN_USER);
        stubWardenBlock();
        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalStudents(3L).totalRooms(4L).occupiedRooms(2L)
                .availableRooms(2L).totalLeaves(1L).pendingLeaves(1L)
                .approvedLeaves(0L).totalComplaints(0L).pendingComplaints(0L)
                .resolvedComplaints(0L).build();
        when(wardenService.getDashboardStats(WARDEN_USER.getId()))
                .thenReturn(ApiResponse.success(stats));

        ReportService.GeneratedReport report =
                reportService.generateSummaryPdf();

        assertPdf(report.content());
        assertEquals("a-wing-senior-boys-dashboard-summary-report.pdf",
                report.filename());
        verify(adminService, never()).getReportSummary();
    }

    @Test
    void studentRoleDenied() {
        User student = User.builder().id(7L).email("s@hostel.com")
                .password("h").role(User.Role.STUDENT).build();
        authenticateAs(student);

        assertThrows(AccessDeniedException.class,
                () -> reportService.generateRoomsPdf());
        verify(roomRepository, never()).findAll();
        verify(roomRepository, never()).findByBlockId(any());
    }
}
