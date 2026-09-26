package com.hostel.service;

import com.hostel.dto.BlockStatsDto;
import com.hostel.dto.DashboardStatsDto;
import com.hostel.entity.AttendanceRecord;
import com.hostel.entity.AttendanceStatus;
import com.hostel.entity.Complaint;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.AttendanceRepository;
import com.hostel.repository.ComplaintRepository;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-side report generation (PDF + Excel).
 *
 * Authorization follows the established two-layer pattern: the controller
 * gates ADMIN/WARDEN roles, and every dataset here is scoped from the
 * authenticated warden's assigned block resolved server-side. ADMIN sees
 * global data. No block or student identifiers are accepted from clients.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Color NAVY = new Color(0x1a, 0x23, 0x7e);
    private static final Color HEADER_BG = new Color(0xe8, 0xea, 0xf6);
    private static final int EXCEL_MAX_COL_CHARS = 40;

    private final UserRepository userRepository;
    private final WardenRepository wardenRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final AttendanceRepository attendanceRepository;
    private final AdminService adminService;
    private final WardenService wardenService;

    public ReportService(UserRepository userRepository,
                         WardenRepository wardenRepository,
                         StudentRepository studentRepository,
                         RoomRepository roomRepository,
                         LeaveRequestRepository leaveRequestRepository,
                         ComplaintRepository complaintRepository,
                         AttendanceRepository attendanceRepository,
                         AdminService adminService,
                         WardenService wardenService) {
        this.userRepository = userRepository;
        this.wardenRepository = wardenRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.complaintRepository = complaintRepository;
        this.attendanceRepository = attendanceRepository;
        this.adminService = adminService;
        this.wardenService = wardenService;
    }

    /** Generated file payload with its download filename. */
    public record GeneratedReport(byte[] content, String filename) {
    }

    private static final class Scope {
        final Long userId;
        final boolean admin;
        final Long blockId;
        final String blockName;

        Scope(Long userId, boolean admin, Long blockId, String blockName) {
            this.userId = userId;
            this.admin = admin;
            this.blockId = blockId;
            this.blockName = blockName;
        }
    }

    private static final class ReportTable {
        final String section;
        final List<String> headers;
        final List<List<Object>> rows;

        ReportTable(String section, List<String> headers, List<List<Object>> rows) {
            this.section = section;
            this.headers = headers;
            this.rows = rows;
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
    }

    private Scope resolveScope() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == User.Role.ADMIN) {
            return new Scope(currentUser.getId(), true, null, null);
        }
        if (currentUser.getRole() == User.Role.WARDEN) {
            Warden warden = wardenRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Warden not found for userId: " + currentUser.getId()));
            HostelBlock block = warden.getBlock();
            if (block == null) {
                throw new AccessDeniedException(
                        "Warden is not assigned to a hostel block");
            }
            return new Scope(currentUser.getId(), false,
                    block.getId(), block.getName());
        }
        throw new AccessDeniedException(
                "You are not authorized to view reports");
    }

    private static String text(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private static String dateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME) : "-";
    }

    private static String fileSlug(String value) {
        String slug = value.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "block" : slug;
    }

    private static String fileName(Scope scope, String base, String extension) {
        String prefix = scope.admin ? "hostel" : fileSlug(scope.blockName);
        return prefix + "-" + base + "-report." + extension;
    }

    private static String roomDisplayStatus(Room room) {
        int occupants = room.getOccupants() == null ? 0 : room.getOccupants();
        int capacity = room.getCapacity() == null ? 0 : room.getCapacity();
        if (occupants >= capacity && capacity > 0 || (capacity <= 0 && occupants > 0)) {
            return "FULL";
        }
        if (room.getStatus() == Room.RoomStatus.MAINTENANCE) {
            return "MAINTENANCE";
        }
        return "AVAILABLE";
    }

    // ============================================================
    // PUBLIC REPORT API
    // ============================================================

    public GeneratedReport generateStudentsPdf() {
        Scope scope = resolveScope();
        ReportTable table = studentTable(scope);
        return new GeneratedReport(
                renderPdf("Student Report", metaLines(scope, "Students"),
                        List.of(table), true),
                fileName(scope, "students", "pdf"));
    }

    public GeneratedReport generateStudentsXlsx() {
        Scope scope = resolveScope();
        ReportTable table = studentTable(scope);
        return new GeneratedReport(
                renderExcel(List.of(sheet("Students", table))),
                fileName(scope, "students", "xlsx"));
    }

    public GeneratedReport generateRoomsPdf() {
        Scope scope = resolveScope();
        ReportTable table = roomTable(scope);
        return new GeneratedReport(
                renderPdf("Room Report", metaLines(scope, "Rooms"),
                        List.of(table), true),
                fileName(scope, "rooms", "pdf"));
    }

    public GeneratedReport generateRoomsXlsx() {
        Scope scope = resolveScope();
        ReportTable table = roomTable(scope);
        return new GeneratedReport(
                renderExcel(List.of(sheet("Rooms", table))),
                fileName(scope, "rooms", "xlsx"));
    }

    public GeneratedReport generateLeavesPdf() {
        Scope scope = resolveScope();
        ReportTable table = leaveTable(scope);
        return new GeneratedReport(
                renderPdf("Leave Report", metaLines(scope, "Leaves"),
                        List.of(table), true),
                fileName(scope, "leaves", "pdf"));
    }

    public GeneratedReport generateLeavesXlsx() {
        Scope scope = resolveScope();
        ReportTable table = leaveTable(scope);
        return new GeneratedReport(
                renderExcel(List.of(sheet("Leaves", table))),
                fileName(scope, "leaves", "xlsx"));
    }

    public GeneratedReport generateComplaintsPdf() {
        Scope scope = resolveScope();
        ReportTable table = complaintTable(scope);
        return new GeneratedReport(
                renderPdf("Complaint Report", metaLines(scope, "Complaints"),
                        List.of(table), true),
                fileName(scope, "complaints", "pdf"));
    }

    public GeneratedReport generateComplaintsXlsx() {
        Scope scope = resolveScope();
        ReportTable table = complaintTable(scope);
        return new GeneratedReport(
                renderExcel(List.of(sheet("Complaints", table))),
                fileName(scope, "complaints", "xlsx"));
    }

    public GeneratedReport generateAttendancePdf(LocalDate date,
                                                 AttendanceStatus status) {
        Scope scope = resolveScope();
        ReportTable table = attendanceTable(scope, date, status);
        List<String> meta = metaLines(scope, "Attendance");
        meta.add("Date: " + (date != null ? date.toString() : "All dates"));
        if (status != null) {
            meta.add("Status: " + status.name());
        }
        return new GeneratedReport(
                renderPdf("Attendance Report", meta,
                        List.of(table), true),
                fileName(scope, "attendance", "pdf"));
    }

    public GeneratedReport generateAttendanceXlsx(LocalDate date,
                                                  AttendanceStatus status) {
        Scope scope = resolveScope();
        ReportTable table = attendanceTable(scope, date, status);
        return new GeneratedReport(
                renderExcel(List.of(sheet("Attendance", table))),
                fileName(scope, "attendance", "xlsx"));
    }

    public GeneratedReport generateSummaryPdf() {
        Scope scope = resolveScope();
        List<ReportTable> tables = summaryTables(scope);
        return new GeneratedReport(
                renderPdf("Hostel Dashboard Summary",
                        metaLines(scope, "Summary"), tables, false),
                fileName(scope, "dashboard-summary", "pdf"));
    }

    public GeneratedReport generateSummaryXlsx() {
        Scope scope = resolveScope();
        List<ReportTable> tables = summaryTables(scope);
        List<SheetData> sheets = new ArrayList<>();
        sheets.add(sheet("Summary", tables.get(0)));
        sheets.add(sheet("Blocks", tables.get(1)));
        return new GeneratedReport(
                renderExcel(sheets),
                fileName(scope, "dashboard-summary", "xlsx"));
    }

    // ============================================================
    // DATASETS (all block-scoped for wardens)
    // ============================================================

    private List<String> metaLines(Scope scope, String type) {
        List<String> meta = new ArrayList<>();
        meta.add("Generated: " + LocalDateTime.now().format(DATE_TIME));
        meta.add("Report: " + type);
        meta.add("Role: " + (scope.admin ? "ADMIN" : "WARDEN"));
        if (!scope.admin) {
            meta.add("Hostel Block: " + scope.blockName);
        }
        return meta;
    }

    private ReportTable studentTable(Scope scope) {
        List<Student> students = scope.admin
                ? studentRepository.findAll()
                : studentRepository.searchByBlock(
                        scope.blockId, null, null, null, Pageable.unpaged())
                        .getContent();

        List<List<Object>> rows = new ArrayList<>();
        for (Student s : students) {
            User u = s.getUser();
            Room room = s.getRoom();
            rows.add(List.of(
                    text(s.getEnrollmentNo()),
                    u != null ? text(u.getName()) : "-",
                    s.getGender() != null ? s.getGender().name() : "-",
                    u != null ? text(u.getEmail()) : "-",
                    u != null ? text(u.getPhone()) : "-",
                    room != null ? text(room.getRoomNo()) : "Not allocated",
                    room != null && room.getBlock() != null
                            ? text(room.getBlock().getName()) : "-"));
        }
        return new ReportTable(null,
                List.of("Enrollment No", "Name", "Gender", "Email", "Phone",
                        "Room", "Block"),
                rows);
    }

    private ReportTable roomTable(Scope scope) {
        List<Room> rooms = scope.admin
                ? roomRepository.findAll()
                : roomRepository.findByBlockId(scope.blockId);

        List<List<Object>> rows = new ArrayList<>();
        for (Room r : rooms) {
            int occupants = r.getOccupants() == null ? 0 : r.getOccupants();
            int capacity = r.getCapacity() == null ? 0 : r.getCapacity();
            rows.add(List.of(
                    r.getBlock() != null ? text(r.getBlock().getName()) : "-",
                    text(r.getRoomNo()),
                    capacity,
                    occupants,
                    Math.max(0, capacity - occupants),
                    roomDisplayStatus(r)));
        }
        return new ReportTable(null,
                List.of("Block", "Room No", "Capacity", "Occupants",
                        "Available Beds", "Status"),
                rows);
    }

    private ReportTable leaveTable(Scope scope) {
        List<LeaveRequest> leaves = scope.admin
                ? leaveRequestRepository.findAllOrderByAppliedAtDesc()
                : leaveRequestRepository
                        .findByBlockIdOrderByAppliedAtDesc(scope.blockId);

        List<List<Object>> rows = new ArrayList<>();
        for (LeaveRequest l : leaves) {
            Student s = l.getStudent();
            User u = s != null ? s.getUser() : null;
            Room room = s != null ? s.getRoom() : null;
            rows.add(List.of(
                    u != null ? text(u.getName()) : "-",
                    room != null ? text(room.getRoomNo()) : "-",
                    room != null && room.getBlock() != null
                            ? text(room.getBlock().getName()) : "-",
                    text(l.getReason()),
                    text(l.getFromDate()),
                    text(l.getToDate()),
                    l.getStatus() != null ? l.getStatus().name() : "-",
                    dateTime(l.getAppliedAt()),
                    dateTime(l.getResolvedAt())));
        }
        return new ReportTable(null,
                List.of("Student", "Room", "Block", "Reason", "From Date",
                        "To Date", "Status", "Applied At", "Resolved At"),
                rows);
    }

    private ReportTable complaintTable(Scope scope) {
        List<Complaint> complaints = scope.admin
                ? complaintRepository.findAllOrderByCreatedAtDesc()
                : complaintRepository
                        .findByBlockIdOrderByCreatedAtDesc(scope.blockId);

        List<List<Object>> rows = new ArrayList<>();
        for (Complaint c : complaints) {
            Student s = c.getStudent();
            User u = s != null ? s.getUser() : null;
            Room room = s != null ? s.getRoom() : null;
            String category = "-";
            if (c.getCategory() != null) {
                category = c.getCategory().name();
            } else if (c.getAiCategory() != null
                    && !c.getAiCategory().isBlank()) {
                category = c.getAiCategory();
            }
            rows.add(List.of(
                    c.getId(),
                    u != null ? text(u.getName()) : "-",
                    room != null ? text(room.getRoomNo()) : "-",
                    room != null && room.getBlock() != null
                            ? text(room.getBlock().getName()) : "-",
                    category,
                    text(c.getTitle()),
                    text(c.getDescription()),
                    c.getStatus() != null ? c.getStatus().name() : "-",
                    dateTime(c.getCreatedAt()),
                    dateTime(c.getResolvedAt())));
        }
        return new ReportTable(null,
                List.of("ID", "Student", "Room", "Block", "Category", "Title",
                        "Description", "Status", "Created At", "Resolved At"),
                rows);
    }

    private ReportTable attendanceTable(Scope scope, LocalDate date,
                                          AttendanceStatus status) {
        List<List<Object>> rows = new ArrayList<>();
        for (AttendanceRecord a : attendanceRows(scope, date, status)) {
            Student s = a.getStudent();
            User u = s != null ? s.getUser() : null;
            Room room = s != null ? s.getRoom() : null;
            rows.add(List.of(
                    u != null ? text(u.getName()) : "-",
                    s != null ? text(s.getEnrollmentNo()) : "-",
                    room != null ? text(room.getRoomNo()) : "-",
                    room != null && room.getBlock() != null
                            ? text(room.getBlock().getName()) : "-",
                    a.getDate() != null ? a.getDate().toString() : "-",
                    a.getStatus() != null ? a.getStatus().name() : "-",
                    text(a.getRemarks()),
                    dateTime(a.getMarkedAt()),
                    a.getMarkedBy() != null
                            ? text(a.getMarkedBy().getName()) : "-"));
        }
        return new ReportTable(null,
                List.of("Student", "Enrollment No", "Room", "Block", "Date",
                        "Status", "Remarks", "Marked At", "Marked By"),
                rows);
    }

    private List<AttendanceRecord> attendanceRows(
            Scope scope, LocalDate date, AttendanceStatus status) {
        if (scope.admin) {
            return attendanceRepository.searchAll(date, status,
                    Pageable.unpaged()).getContent();
        }
        return attendanceRepository.searchByBlock(scope.blockId, date, status,
                Pageable.unpaged()).getContent();
    }

    private List<ReportTable> summaryTables(Scope scope) {
        List<List<Object>> metrics = new ArrayList<>();
        List<List<Object>> blocks = new ArrayList<>();

        if (scope.admin) {
            Map<String, Object> summary = adminService.getReportSummary();
            DashboardStatsDto stats =
                    adminService.getDashboardStats().getData();
            metric(metrics, "Total Students", summary.get("totalStudents"));
            metric(metrics, "Total Wardens", summary.get("totalWardens"));
            metric(metrics, "Total Rooms", summary.get("totalRooms"));
            metric(metrics, "Total Blocks", summary.get("totalBlocks"));
            metric(metrics, "Occupancy Rate (%)", summary.get("occupancyRate"));
            metric(metrics, "Rooms with Rent", summary.get("roomsWithRent"));
            metric(metrics, "Average Rent", summary.get("averageRent"));
            metric(metrics, "Active Students", summary.get("activeStudents"));
            metric(metrics, "Total Leaves", stats.getTotalLeaves());
            metric(metrics, "Pending Leaves", stats.getPendingLeaves());
            metric(metrics, "Approved Leaves", stats.getApprovedLeaves());
            metric(metrics, "Total Complaints", stats.getTotalComplaints());
            metric(metrics, "Pending Complaints", stats.getPendingComplaints());
            metric(metrics, "Resolved Complaints", stats.getResolvedComplaints());
            if (stats.getBlockStats() != null) {
                for (BlockStatsDto b : stats.getBlockStats()) {
                    blocks.add(List.of(text(b.getName()), b.getCapacity(),
                            b.getOccupied()));
                }
            }
        } else {
            DashboardStatsDto stats = wardenService
                    .getDashboardStats(scope.userId).getData();
            metric(metrics, "Hostel Block", scope.blockName);
            metric(metrics, "Total Students", stats.getTotalStudents());
            metric(metrics, "Total Rooms", stats.getTotalRooms());
            metric(metrics, "Occupied Rooms", stats.getOccupiedRooms());
            metric(metrics, "Available Rooms", stats.getAvailableRooms());
            metric(metrics, "Total Leaves", stats.getTotalLeaves());
            metric(metrics, "Pending Leaves", stats.getPendingLeaves());
            metric(metrics, "Approved Leaves", stats.getApprovedLeaves());
            metric(metrics, "Total Complaints", stats.getTotalComplaints());
            metric(metrics, "Pending Complaints", stats.getPendingComplaints());
            metric(metrics, "Resolved Complaints", stats.getResolvedComplaints());
            blocks.add(List.of(text(scope.blockName), stats.getTotalRooms(),
                    stats.getOccupiedRooms()));
        }

        return List.of(
                new ReportTable("Key Metrics", List.of("Metric", "Value"), metrics),
                new ReportTable("Block Occupancy",
                        List.of("Block", "Total Rooms", "Occupied Rooms"), blocks));
    }

    private static void metric(List<List<Object>> metrics, String name, Object value) {
        metrics.add(List.of(name, value != null ? value : "-"));
    }

    // ============================================================
    // PDF RENDERING (OpenPDF)
    // ============================================================

    private static final class PageNumbers extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Phrase footer = new Phrase("Page " + writer.getPageNumber(),
                    new Font(Font.HELVETICA, 9, Font.NORMAL,
                            new Color(0x75, 0x75, 0x75)));
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, footer,
                    (document.right() + document.left()) / 2,
                    document.bottom() - 20, 0);
        }
    }

    private byte[] renderPdf(String title, List<String> meta,
                             List<ReportTable> tables, boolean landscape) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(
                    landscape ? PageSize.A4.rotate() : PageSize.A4,
                    36, 36, 48, 48);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumbers());
            document.open();

            Paragraph heading = new Paragraph(title,
                    new Font(Font.HELVETICA, 16, Font.BOLD, NAVY));
            heading.setSpacingAfter(6);
            document.add(heading);

            Font metaFont = new Font(Font.HELVETICA, 10, Font.NORMAL,
                    new Color(0x61, 0x61, 0x61));
            for (String line : meta) {
                Paragraph metaPara = new Paragraph(line, metaFont);
                metaPara.setSpacingAfter(2);
                document.add(metaPara);
            }
            document.add(new Paragraph(" "));

            for (ReportTable table : tables) {
                if (table.section != null) {
                    Paragraph section = new Paragraph(table.section,
                            new Font(Font.HELVETICA, 12, Font.BOLD, NAVY));
                    section.setSpacingBefore(8);
                    section.setSpacingAfter(6);
                    document.add(section);
                }
                PdfPTable pdfTable = new PdfPTable(table.headers.size());
                pdfTable.setWidthPercentage(100);
                pdfTable.setHeaderRows(1);
                Font headerFont =
                        new Font(Font.HELVETICA, 9, Font.BOLD);
                for (String header : table.headers) {
                    PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
                    headerCell.setBackgroundColor(HEADER_BG);
                    headerCell.setPadding(5);
                    pdfTable.addCell(headerCell);
                }
                Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
                for (List<Object> row : table.rows) {
                    for (Object value : row) {
                        PdfPCell cell = new PdfPCell(new Phrase(
                                value != null ? value.toString() : "-",
                                cellFont));
                        cell.setPadding(4);
                        pdfTable.addCell(cell);
                    }
                }
                document.add(pdfTable);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate PDF report", e);
        }
    }

    // ============================================================
    // EXCEL RENDERING (Apache POI)
    // ============================================================

    private static final class SheetData {
        final String name;
        final ReportTable table;

        SheetData(String name, ReportTable table) {
            this.name = name;
            this.table = table;
        }
    }

    private SheetData sheet(String name, ReportTable table) {
        return new SheetData(name, table);
    }

    private byte[] renderExcel(List<SheetData> sheets) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont =
                    workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (SheetData sheetData : sheets) {
                Sheet sheet = workbook.createSheet(sheetData.name);
                int colCount = sheetData.table.headers.size();

                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < colCount; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(sheetData.table.headers.get(i));
                    cell.setCellStyle(headerStyle);
                }

                int rowIndex = 1;
                for (List<Object> row : sheetData.table.rows) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    for (int i = 0; i < colCount; i++) {
                        Object value = i < row.size() ? row.get(i) : null;
                        Cell cell = dataRow.createCell(i);
                        if (value instanceof Number number) {
                            cell.setCellValue(number.doubleValue());
                        } else {
                            cell.setCellValue(
                                    value != null ? value.toString() : "-");
                        }
                    }
                }

                sheet.createFreezePane(0, 1);
                if (rowIndex > 1) {
                    sheet.setAutoFilter(new CellRangeAddress(
                            0, rowIndex - 1, 0, colCount - 1));
                }
                for (int i = 0; i < colCount; i++) {
                    sheet.autoSizeColumn(i);
                    if (sheet.getColumnWidth(i) > EXCEL_MAX_COL_CHARS * 256) {
                        sheet.setColumnWidth(i, EXCEL_MAX_COL_CHARS * 256);
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate Excel report", e);
        }
    }
}
