package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.RoomDto;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.HostelBlockRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.springframework.security.access.AccessDeniedException;
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
    private final UserRepository userRepository;
    private final WardenRepository wardenRepository;

    public RoomService(RoomRepository roomRepository,
                       HostelBlockRepository hostelBlockRepository,
                       StudentRepository studentRepository,
                       EmailService emailService,
                       AuditService auditService,
                       UserRepository userRepository,
                       WardenRepository wardenRepository) {
        this.roomRepository = roomRepository;
        this.hostelBlockRepository = hostelBlockRepository;
        this.studentRepository = studentRepository;
        this.emailService = emailService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.wardenRepository = wardenRepository;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private User getCurrentUser() {
        String currentUserEmail = getCurrentUserEmail();
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));
    }

    /**
     * Resolves the assigned block of the given warden user.
     * Used to scope room READ operations to the warden's own block.
     */
    private Long getWardenBlockId(Long wardenUserId) {
        Warden warden = wardenRepository.findByUserId(wardenUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Warden not found for userId: " + wardenUserId));

        if (warden.getBlock() == null) {
            throw new AccessDeniedException(
                    "Warden is not assigned to a hostel block");
        }

        return warden.getBlock().getId();
    }

    /**
     * WARDEN callers may only manage rooms inside their own assigned block.
     * ADMIN (and any other role already permitted by the controller) passes through unchanged.
     */
    private void verifyWardenRoomAccess(Room room) {

        String currentUserEmail = getCurrentUserEmail();

        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + currentUserEmail));

        if (currentUser.getRole() != User.Role.WARDEN) {
            return;
        }

        Warden warden = wardenRepository.findByUserId(currentUser.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Warden not found for userId: " + currentUser.getId()));

        if (warden.getBlock() == null) {
            throw new AccessDeniedException(
                    "Warden is not assigned to a hostel block");
        }

        if (room.getBlock() == null) {
            throw new AccessDeniedException(
                    "Room is not assigned to a hostel block");
        }

        if (!warden.getBlock().getId().equals(room.getBlock().getId())) {
            throw new AccessDeniedException(
                    "You are not authorized to manage rooms in another block");
        }
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
        User currentUser = getCurrentUser();

        List<Room> rooms;

        if (currentUser.getRole() == User.Role.WARDEN) {
            rooms = roomRepository.findByBlockId(
                    getWardenBlockId(currentUser.getId()));
        } else if (currentUser.getRole() == User.Role.ADMIN) {
            rooms = roomRepository.findAll();
        } else {
            throw new AccessDeniedException(
                    "You are not authorized to view rooms");
        }

        List<RoomDto> dtos = rooms.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<RoomDto> getRoomById(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.WARDEN) {
            verifyWardenRoomAccess(room);
        } else if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException(
                    "You are not authorized to view rooms");
        }

        return ApiResponse.success(mapToDto(room));
    }

    public ApiResponse<List<RoomDto>> getRoomsByBlock(Long blockId) {
        User currentUser = getCurrentUser();

        List<Room> rooms;

        if (currentUser.getRole() == User.Role.WARDEN) {
            Long ownBlockId = getWardenBlockId(currentUser.getId());
            if (!ownBlockId.equals(blockId)) {
                throw new AccessDeniedException(
                        "You are not authorized to view rooms in another block");
            }
            rooms = roomRepository.findByBlockId(ownBlockId);
        } else if (currentUser.getRole() == User.Role.ADMIN) {
            rooms = roomRepository.findByBlockId(blockId);
        } else {
            throw new AccessDeniedException(
                    "You are not authorized to view rooms");
        }

        List<RoomDto> dtos = rooms.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<RoomDto>> getAvailableRooms() {
        User currentUser = getCurrentUser();

        List<Room> rooms;

        if (currentUser.getRole() == User.Role.WARDEN) {
            rooms = roomRepository.findByStatusAndBlockId(
                    Room.RoomStatus.AVAILABLE,
                    getWardenBlockId(currentUser.getId()));
        } else if (currentUser.getRole() == User.Role.ADMIN) {
            rooms = roomRepository.findAvailableRooms();
        } else {
            throw new AccessDeniedException(
                    "You are not authorized to view rooms");
        }

        List<RoomDto> dtos = rooms.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Void> allocateRoom(Long roomId, Long studentId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));

        verifyWardenRoomAccess(room);

        if (room.getStatus() == Room.RoomStatus.MAINTENANCE) {
            throw new BadRequestException("Room is under maintenance and cannot be allocated");
        }

        int currentOccupants = room.getOccupants() == null ? 0 : room.getOccupants();
        if (currentOccupants >= room.getCapacity()) {
            throw new BadRequestException("Room is at full capacity");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        if (student.getRoom() != null) {
            throw new BadRequestException("Student is already assigned to a room. Please vacate the current room first.");
        }

        student.setRoom(room);
        studentRepository.save(student);

        room.setOccupants(currentOccupants + 1);
        if (room.getOccupants() >= room.getCapacity()) {
            room.setStatus(Room.RoomStatus.OCCUPIED);
        } else {
            room.setStatus(Room.RoomStatus.AVAILABLE);
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

        verifyWardenRoomAccess(room);

        String roomNo = room.getRoomNo();
        String blockName = room.getBlock().getName();
        String studentEmail = student.getUser().getEmail();
        String studentName = student.getUser().getName();

        student.setRoom(null);
        studentRepository.save(student);

        int remainingOccupants = room.getOccupants() == null
                ? 0
                : Math.max(0, room.getOccupants() - 1);
        room.setOccupants(remainingOccupants);
        if (room.getStatus() != Room.RoomStatus.MAINTENANCE) {
            room.setStatus(remainingOccupants < room.getCapacity()
                    ? Room.RoomStatus.AVAILABLE
                    : Room.RoomStatus.OCCUPIED);
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
