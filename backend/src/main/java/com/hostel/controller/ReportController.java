package com.hostel.controller;

import com.hostel.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Reports", description = "PDF and Excel report downloads")
public class ReportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    private ResponseEntity<byte[]> download(
            ReportService.GeneratedReport report, MediaType mediaType) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + report.filename() + "\"")
                .body(report.content());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/students.pdf", produces = "application/pdf")
    @Operation(summary = "Download student report as PDF")
    public ResponseEntity<byte[]> studentsPdf() {
        return download(reportService.generateStudentsPdf(),
                MediaType.APPLICATION_PDF);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/students.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download student report as Excel")
    public ResponseEntity<byte[]> studentsXlsx() {
        return download(reportService.generateStudentsXlsx(),
                XLSX_MEDIA_TYPE);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/rooms.pdf", produces = "application/pdf")
    @Operation(summary = "Download room report as PDF")
    public ResponseEntity<byte[]> roomsPdf() {
        return download(reportService.generateRoomsPdf(),
                MediaType.APPLICATION_PDF);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/rooms.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download room report as Excel")
    public ResponseEntity<byte[]> roomsXlsx() {
        return download(reportService.generateRoomsXlsx(),
                XLSX_MEDIA_TYPE);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/leaves.pdf", produces = "application/pdf")
    @Operation(summary = "Download leave report as PDF")
    public ResponseEntity<byte[]> leavesPdf() {
        return download(reportService.generateLeavesPdf(),
                MediaType.APPLICATION_PDF);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/leaves.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download leave report as Excel")
    public ResponseEntity<byte[]> leavesXlsx() {
        return download(reportService.generateLeavesXlsx(),
                XLSX_MEDIA_TYPE);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/complaints.pdf", produces = "application/pdf")
    @Operation(summary = "Download complaint report as PDF")
    public ResponseEntity<byte[]> complaintsPdf() {
        return download(reportService.generateComplaintsPdf(),
                MediaType.APPLICATION_PDF);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/complaints.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download complaint report as Excel")
    public ResponseEntity<byte[]> complaintsXlsx() {
        return download(reportService.generateComplaintsXlsx(),
                XLSX_MEDIA_TYPE);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/dashboard-summary.pdf", produces = "application/pdf")
    @Operation(summary = "Download dashboard summary as PDF")
    public ResponseEntity<byte[]> summaryPdf() {
        return download(reportService.generateSummaryPdf(),
                MediaType.APPLICATION_PDF);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping(value = "/dashboard-summary.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Download dashboard summary as Excel")
    public ResponseEntity<byte[]> summaryXlsx() {
        return download(reportService.generateSummaryXlsx(),
                XLSX_MEDIA_TYPE);
    }
}
