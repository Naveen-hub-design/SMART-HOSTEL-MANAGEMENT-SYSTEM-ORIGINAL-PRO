package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.AuthResponse;
import com.hostel.dto.DashboardStatsDto;
import com.hostel.dto.RegisterRequest;
import com.hostel.dto.StudentProfileDto;
import com.hostel.entity.Complaint;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Room;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.entity.Student;
import com.hostel.exception.DuplicateResourceException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.AdminRepository;
import com.hostel.repository.ComplaintRepository;
import com.hostel.repository.HostelBlockRepository;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.LostAndFoundRepository;
import com.hostel.repository.MarketplaceItemRepository;
import com.hostel.repository.MessFeedbackRepository;
import com.hostel.repository.NoticeRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import com.hostel.security.JwtUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final WardenRepository wardenRepository;
    private final HostelBlockRepository hostelBlockRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final NoticeRepository noticeRepository;
    private final MarketplaceItemRepository marketplaceItemRepository;
    private final LostAndFoundRepository lostAndFoundRepository;
    private final MessFeedbackRepository messFeedbackRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;

    public AdminService(UserRepository userRepository,
                        AdminRepository adminRepository,
                        WardenRepository wardenRepository,
                        HostelBlockRepository hostelBlockRepository,
                        RoomRepository roomRepository,
                        StudentRepository studentRepository,
                        LeaveRequestRepository leaveRequestRepository,
                        ComplaintRepository complaintRepository,
                        NoticeRepository noticeRepository,
                        MarketplaceItemRepository marketplaceItemRepository,
                        LostAndFoundRepository lostAndFoundRepository,
                        MessFeedbackRepository messFeedbackRepository,
                        BCryptPasswordEncoder passwordEncoder,
                        JwtUtils jwtUtils,
                        AuditService auditService) {
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.wardenRepository = wardenRepository;
        this.hostelBlockRepository = hostelBlockRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.complaintRepository = complaintRepository;
        this.noticeRepository = noticeRepository;
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.lostAndFoundRepository = lostAndFoundRepository;
        this.messFeedbackRepository = messFeedbackRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.auditService = auditService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public ApiResponse<DashboardStatsDto> getDashboardStats() {
        long totalStudents = studentRepository.count();
        long totalRooms = roomRepository.count();
        long occupiedRooms = roomRepository.countOccupiedRooms();
        long availableRooms = roomRepository.countAvailableRooms();
        long totalComplaints = complaintRepository.count();
        long pendingComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.PENDING);
        long resolvedComplaints = complaintRepository.countByStatus(Complaint.ComplaintStatus.RESOLVED);
        long totalLeaves = leaveRequestRepository.count();
        long pendingLeaves = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.PENDING);
        long approvedLeaves = leaveRequestRepository.countByStatus(LeaveRequest.LeaveStatus.APPROVED);

        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalStudents(totalStudents)
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .availableRooms(availableRooms)
                .totalComplaints(totalComplaints)
                .pendingComplaints(pendingComplaints)
                .resolvedComplaints(resolvedComplaints)
                .totalLeaves(totalLeaves)
                .pendingLeaves(pendingLeaves)
                .approvedLeaves(approvedLeaves)
                .build();

        return ApiResponse.success(stats);
    }

    public ApiResponse<AuthResponse> createWarden(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.WARDEN)
                .phone(request.getPhone())
                .build();
        user = userRepository.save(user);

        Warden warden = Warden.builder()
                .user(user)
                .qualification(request.getQualification())
                .build();
        wardenRepository.save(warden);

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .name(user.getName())
                .email(user.getEmail())
                .userId(user.getId())
                .message("Warden created successfully")
                .build();

        auditService.logAction("WARDEN_CREATED", getCurrentUserEmail(), "ADMIN", "USER", user.getId(),
                "Warden created with email: " + user.getEmail());
        return ApiResponse.success("Warden created successfully", authResponse);
    }

    public ApiResponse<Void> deleteWarden(Long wardenId) {
        Warden warden = wardenRepository.findById(wardenId)
                .orElseThrow(() -> new ResourceNotFoundException("Warden", wardenId));

        User user = warden.getUser();
        wardenRepository.delete(warden);
        userRepository.delete(user);

        auditService.logAction("WARDEN_DELETED", getCurrentUserEmail(), "ADMIN", "USER", wardenId,
                "Warden deleted: " + user.getEmail());
        return ApiResponse.success("Warden deleted successfully", null);
    }

    public ApiResponse<List<Map<String, Object>>> getAllWardens() {
        List<Warden> wardens = wardenRepository.findAll();
        List<Map<String, Object>> wardenList = new ArrayList<>();

        for (Warden warden : wardens) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", warden.getId());
            map.put("name", warden.getUser().getName());
            map.put("email", warden.getUser().getEmail());
            map.put("phone", warden.getUser().getPhone());
            map.put("qualification", warden.getQualification());
            map.put("blockName", warden.getBlock() != null ? warden.getBlock().getName() : null);
            map.put("blockId", warden.getBlock() != null ? warden.getBlock().getId() : null);
            wardenList.add(map);
        }

        return ApiResponse.success(wardenList);
    }

    public ApiResponse<List<StudentProfileDto>> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        List<StudentProfileDto> dtos = students.stream().map(student -> {
            User user = student.getUser();
            return StudentProfileDto.builder()
                    .id(student.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .enrollmentNo(student.getEnrollmentNo())
                    .parentContact(student.getParentContact())
                    .address(student.getAddress())
                    .dateOfBirth(student.getDateOfBirth())
                    .gender(student.getGender() != null ? student.getGender().name() : null)
                    .profileImageUrl(student.getProfileImageUrl())
                    .roomNo(student.getRoom() != null ? student.getRoom().getRoomNo() : null)
                    .blockName(student.getRoom() != null ? student.getRoom().getBlock().getName() : null)
                    .build();
        }).collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Void> createHostelBlock(String name, String code, String address) {
        if (hostelBlockRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Hostel block with code " + code + " already exists");
        }

        HostelBlock block = HostelBlock.builder()
                .name(name)
                .code(code)
                .address(address)
                .build();
        hostelBlockRepository.save(block);

        auditService.logAction("BLOCK_CREATED", getCurrentUserEmail(), "ADMIN", "HOSTEL_BLOCK", block.getId(),
                "Hostel block created: " + block.getName() + " (" + block.getCode() + ")");
        return ApiResponse.success("Hostel block created successfully", null);
    }

    public ApiResponse<List<Map<String, Object>>> getAllHostelBlocks() {
        List<HostelBlock> blocks = hostelBlockRepository.findAll();
        List<Map<String, Object>> blockList = new ArrayList<>();

        for (HostelBlock block : blocks) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", block.getId());
            map.put("name", block.getName());
            map.put("code", block.getCode());
            map.put("address", block.getAddress());
            map.put("roomCount", block.getRooms() != null ? block.getRooms().size() : 0);

            Warden warden = wardenRepository.findByBlockId(block.getId()).orElse(null);
            map.put("wardenName", warden != null ? warden.getUser().getName() : null);
            map.put("wardenId", warden != null ? warden.getId() : null);

            blockList.add(map);
        }

        return ApiResponse.success(blockList);
    }

    public ApiResponse<Void> deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        if (student.getRoom() != null) {
            Room room = student.getRoom();
            room.setOccupants(Math.max(0, (room.getOccupants() != null ? room.getOccupants() : 1) - 1));
            if (room.getOccupants() < room.getCapacity()) {
                room.setStatus(Room.RoomStatus.AVAILABLE);
            }
            roomRepository.save(room);
            student.setRoom(null);
        }

        complaintRepository.findByStudentId(studentId)
                .forEach(c -> complaintRepository.delete(c));
        leaveRequestRepository.findByStudentId(studentId)
                .forEach(l -> leaveRequestRepository.delete(l));
        messFeedbackRepository.findByStudentId(studentId)
                .forEach(m -> messFeedbackRepository.delete(m));
        marketplaceItemRepository.findBySellerId(studentId)
                .forEach(i -> marketplaceItemRepository.delete(i));
        lostAndFoundRepository.findByReportedById(studentId)
                .forEach(l -> lostAndFoundRepository.delete(l));

        User user = student.getUser();
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }

        auditService.logAction("STUDENT_DELETED", getCurrentUserEmail(), "ADMIN", "STUDENT", studentId,
                "Student deleted: " + (user != null ? user.getEmail() : studentId));
        return ApiResponse.success("Student deleted successfully", null);
    }
}
