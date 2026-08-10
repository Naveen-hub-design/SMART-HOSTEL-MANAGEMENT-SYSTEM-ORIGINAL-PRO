package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.RoomDto;
import com.hostel.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Rooms", description = "Room allocation and management")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Operation(summary = "Add new room")
    public ResponseEntity<ApiResponse<RoomDto>> addRoom(@Valid @RequestBody RoomDto roomDto) {
        return ResponseEntity.ok(roomService.addRoom(roomDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update room details")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(@PathVariable Long id,
                                                             @Valid @RequestBody RoomDto roomDto) {
        return ResponseEntity.ok(roomService.updateRoom(id, roomDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete room")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.deleteRoom(id));
    }

    @GetMapping
    @Operation(summary = "Get all rooms")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @GetMapping("/block/{blockId}")
    @Operation(summary = "Get rooms by block")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRoomsByBlock(@PathVariable Long blockId) {
        return ResponseEntity.ok(roomService.getRoomsByBlock(blockId));
    }

    @GetMapping("/available")
    @Operation(summary = "Get available rooms")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAvailableRooms() {
        return ResponseEntity.ok(roomService.getAvailableRooms());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @PostMapping("/{roomId}/allocate/{studentId}")
    @Operation(summary = "Allocate room to student")
    public ResponseEntity<ApiResponse<Void>> allocateRoom(@PathVariable Long roomId,
                                                           @PathVariable Long studentId) {
        return ResponseEntity.ok(roomService.allocateRoom(roomId, studentId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    @PostMapping("/vacate/{studentId}")
    @Operation(summary = "Vacate student's room")
    public ResponseEntity<ApiResponse<Void>> vacateRoom(@PathVariable Long studentId) {
        return ResponseEntity.ok(roomService.vacateRoom(studentId));
    }
}
