package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.AttendanceBulkRequest;
import com.hostel.dto.AttendanceBulkResultDto;
import com.hostel.dto.AttendanceCorrectionRequest;
import com.hostel.dto.AttendanceDto;
import com.hostel.dto.AttendanceMarkRequest;
import com.hostel.dto.PageResponse;
import com.hostel.entity.AttendanceStatus;
import com.hostel.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Attendance", description = "Student attendance marking and review")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @PostMapping("/mark")
    @Operation(summary = "Mark single student attendance")
    public ResponseEntity<ApiResponse<AttendanceDto>> markAttendance(
            @Valid @RequestBody AttendanceMarkRequest request) {
        return ResponseEntity.ok(attendanceService.markAttendance(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @PostMapping("/mark-bulk")
    @Operation(summary = "Mark bulk attendance with per-row isolation")
    public ResponseEntity<ApiResponse<AttendanceBulkResultDto>> bulkMarkAttendance(
            @Valid @RequestBody AttendanceBulkRequest request) {
        return ResponseEntity.ok(attendanceService.bulkMarkAttendance(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @GetMapping
    @Operation(summary = "List attendance (block-scoped for wardens)")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceDto>>> listAttendance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(attendanceService.searchAttendance(
                date, status, page, size));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    @Operation(summary = "Get own attendance")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceDto>>> getMyAttendance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(attendanceService.getMyAttendance(
                date, status, page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @PutMapping("/{id}")
    @Operation(summary = "Correct attendance status and remarks")
    public ResponseEntity<ApiResponse<AttendanceDto>> correctAttendance(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceCorrectionRequest request) {
        return ResponseEntity.ok(
                attendanceService.correctAttendance(id, request));
    }
}
