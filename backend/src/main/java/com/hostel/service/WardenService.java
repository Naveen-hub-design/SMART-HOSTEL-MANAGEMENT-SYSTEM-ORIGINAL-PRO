package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.DashboardStatsDto;
import com.hostel.dto.StudentProfileDto;
import com.hostel.entity.Complaint;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.ComplaintRepository;
import com.hostel.repository.HostelBlockRepository;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.NoticeRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class WardenService {

    private final WardenRepository wardenRepository;
    private final UserRepository userRepository;
    private final HostelBlockRepository hostelBlockRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final NoticeRepository noticeRepository;

    public WardenService(WardenRepository wardenRepository,
                         UserRepository userRepository,
                         HostelBlockRepository hostelBlockRepository,
                         RoomRepository roomRepository,
                         StudentRepository studentRepository,
                         LeaveRequestRepository leaveRequestRepository,
                         ComplaintRepository complaintRepository,
                         NoticeRepository noticeRepository) {
        this.wardenRepository = wardenRepository;
        this.userRepository = userRepository;
        this.hostelBlockRepository = hostelBlockRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.complaintRepository = complaintRepository;
        this.noticeRepository = noticeRepository;
    }

    public ApiResponse<DashboardStatsDto> getDashboardStats(Long wardenUserId) {
        Warden warden = wardenRepository.findByUserId(wardenUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Warden not found for userId: " + wardenUserId));

        HostelBlock block = warden.getBlock();
        if (block == null) {
            DashboardStatsDto empty = DashboardStatsDto.builder()
                    .totalStudents(0).totalRooms(0).occupiedRooms(0)
                    .availableRooms(0).totalComplaints(0).pendingComplaints(0)
                    .resolvedComplaints(0).totalLeaves(0).pendingLeaves(0)
                    .approvedLeaves(0).build();
            return ApiResponse.success(empty);
        }

        List<Room> blockRooms = roomRepository.findByBlockId(block.getId());
        long totalRooms = blockRooms.size();
        long occupiedRooms = blockRooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.OCCUPIED).count();
        long availableRooms = blockRooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.AVAILABLE).count();

        List<Student> blockStudents = new ArrayList<>();
        for (Room room : blockRooms) {
            blockStudents.addAll(studentRepository.findByRoom(room));
        }
        long totalStudents = blockStudents.size();

        long pendingLeaves = 0;
        long pendingComplaints = 0;
        for (Student s : blockStudents) {
            pendingLeaves += leaveRequestRepository.findByStudentId(s.getId()).stream()
                    .filter(lr -> lr.getStatus() == LeaveRequest.LeaveStatus.PENDING).count();
            pendingComplaints += complaintRepository.findByStudentId(s.getId()).stream()
                    .filter(c -> c.getStatus() == Complaint.ComplaintStatus.PENDING).count();
        }

        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalStudents(totalStudents)
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .availableRooms(availableRooms)
                .pendingLeaves(pendingLeaves)
                .pendingComplaints(pendingComplaints)
                .build();

        return ApiResponse.success(stats);
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

    public ApiResponse<Void> assignWardenToBlock(Long wardenId, Long blockId) {
        Warden warden = wardenRepository.findById(wardenId)
                .orElseThrow(() -> new ResourceNotFoundException("Warden", wardenId));

        HostelBlock block = hostelBlockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("HostelBlock", blockId));

        warden.setBlock(block);
        wardenRepository.save(warden);

        return ApiResponse.success("Warden assigned to block successfully", null);
    }

    public ApiResponse<List<StudentProfileDto>> getStudentsByWardenBlock(Long wardenUserId) {
        Warden warden = wardenRepository.findByUserId(wardenUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Warden not found for userId: " + wardenUserId));

        HostelBlock block = warden.getBlock();
        if (block == null) {
            return ApiResponse.success(new ArrayList<>());
        }

        List<Room> blockRooms = roomRepository.findByBlockId(block.getId());
        List<Student> blockStudents = new ArrayList<>();
        for (Room room : blockRooms) {
            blockStudents.addAll(studentRepository.findByRoom(room));
        }

        List<StudentProfileDto> dtos = blockStudents.stream().map(student -> {
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
}
