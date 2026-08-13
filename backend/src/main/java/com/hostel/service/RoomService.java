package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.RoomDto;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.HostelBlockRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final HostelBlockRepository hostelBlockRepository;
    private final StudentRepository studentRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    public RoomService(RoomRepository roomRepository,
                       HostelBlockRepository hostelBlockRepository,
                       StudentRepository studentRepository,
                       EmailService emailService,
                       AuditService auditService) {
        this.roomRepository = roomRepository;
        this.hostelBlockRepository = hostelBlockRepository;
        this.studentRepository = studentRepository;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public ApiResponse<RoomDto> addRoom(RoomDto roomDto) {
        HostelBlock block = hostelBlockRepository.findByName(roomDto.getBlockName())
                .orElseThrow(() -> new ResourceNotFoundException("HostelBlock not found with name: " + roomDto.getBlockName()));

        if (roomRepository.findByRoomNoAndBlockId(roomDto.getRoomNo(), block.getId()).isPresent()) {
            throw new BadRequestException("Room number already exists in this block");
        }

        Room room = Room.builder()
                .roomNo(roomDto.getRoomNo())
                .block(block)
                .floor(roomDto.getFloor())
                .capacity(roomDto.getCapacity())
                .occupants(0)
                .status(Room.RoomStatus.AVAILABLE)
                .rent(roomDto.getRent())
                .build();
        room = roomRepository.save(room);

        return ApiResponse.success("Room added successfully", mapToDto(room));
    }

    public ApiResponse<RoomDto> updateRoom(Long roomId, RoomDto roomDto) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        room.setRoomNo(roomDto.getRoomNo());
        room.setFloor(roomDto.getFloor());
        room.setCapacity(roomDto.getCapacity());
        if (roomDto.getStatus() != null) {
            room.setStatus(Room.RoomStatus.valueOf(roomDto.getStatus().toUpperCase()));
        }
        room.setRent(roomDto.getRent());

        if (roomDto.getOccupants() != null) {
            room.setOccupants(roomDto.getOccupants());
        }

        roomRepository.save(room);
        return ApiResponse.success("Room updated successfully", mapToDto(room));
    }

    public ApiResponse<Void> deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        List<Student> occupants = studentRepository.findByRoomId(roomId);
        if (!occupants.isEmpty()) {
            throw new BadRequestException("Cannot delete room with " + occupants.size() + " occupant(s). Please vacate the room first.");
        }

        roomRepository.delete(room);
        return ApiResponse.success("Room deleted successfully", null);
    }

    public ApiResponse<List<RoomDto>> getAllRooms() {
        List<Room> rooms = roomRepository.findAll();
        List<RoomDto> dtos = rooms.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<RoomDto> getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        return ApiResponse.success(mapToDto(room));
    }

    public ApiResponse<List<RoomDto>> getRoomsByBlock(Long blockId) {
        List<Room> rooms = roomRepository.findByBlockId(blockId);
        List<RoomDto> dtos = rooms.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<RoomDto>> getAvailableRooms() {
        List<Room> rooms = roomRepository.findAvailableRooms();
        List<RoomDto> dtos = rooms.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Void> allocateRoom(Long roomId, Long studentId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        if (room.getStatus() != Room.RoomStatus.AVAILABLE) {
            throw new BadRequestException("Room is not available for allocation");
        }

        if (room.getOccupants() >= room.getCapacity()) {
            throw new BadRequestException("Room is at full capacity");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        if (student.getRoom() != null) {
            throw new BadRequestException("Student is already assigned to a room. Please vacate the current room first.");
        }

        student.setRoom(room);
        studentRepository.save(student);

        room.setOccupants(room.getOccupants() == null ? 1 : room.getOccupants() + 1);
        if (room.getOccupants() >= room.getCapacity()) {
            room.setStatus(Room.RoomStatus.OCCUPIED);
        }
        roomRepository.save(room);

        emailService.sendRoomAllocation(
                student.getUser().getEmail(),
                student.getUser().getName(),
                room.getRoomNo(),
                room.getBlock().getName()
        );

        auditService.logAction("ROOM_ALLOCATED", getCurrentUserEmail(), null, "ROOM", room.getId(),
                "Room " + room.getRoomNo() + " allocated to student " + student.getUser().getEmail());
        return ApiResponse.success("Room allocated successfully", null);
    }

    public ApiResponse<Void> vacateRoom(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        if (student.getRoom() == null) {
            throw new BadRequestException("Student does not have a room assigned");
        }

        Room room = student.getRoom();
        String roomNo = room.getRoomNo();
        String blockName = room.getBlock().getName();
        String studentEmail = student.getUser().getEmail();
        String studentName = student.getUser().getName();

        student.setRoom(null);
        studentRepository.save(student);

        room.setOccupants(Math.max(0, room.getOccupants() - 1));
        if (room.getOccupants() == 0) {
            room.setStatus(Room.RoomStatus.AVAILABLE);
        }
        roomRepository.save(room);

        emailService.sendRoomAllocation(studentEmail, studentName, roomNo, blockName);

        auditService.logAction("ROOM_VACATED", getCurrentUserEmail(), null, "ROOM", room.getId(),
                "Room " + roomNo + " vacated by student " + studentEmail);
        return ApiResponse.success("Room vacated successfully", null);
    }

    private RoomDto mapToDto(Room room) {
        return RoomDto.builder()
                .id(room.getId())
                .roomNo(room.getRoomNo())
                .blockName(room.getBlock() != null ? room.getBlock().getName() : null)
                .blockId(room.getBlock() != null ? room.getBlock().getId() : null)
                .floor(room.getFloor())
                .capacity(room.getCapacity())
                .occupants(room.getOccupants())
                .status(room.getStatus() != null ? room.getStatus().name() : null)
                .rent(room.getRent())
                .build();
    }
}
