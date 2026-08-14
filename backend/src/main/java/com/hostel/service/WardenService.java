package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.AuthResponse;
import com.hostel.dto.BulkImportResultDto;
import com.hostel.dto.DashboardStatsDto;
import com.hostel.dto.RegisterRequest;
import com.hostel.dto.StudentProfileDto;

import com.hostel.entity.Complaint;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.LeaveRequest;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;

import com.hostel.exception.BadRequestException;
import com.hostel.exception.DuplicateResourceException;
import com.hostel.exception.ResourceNotFoundException;

import com.hostel.repository.ComplaintRepository;
import com.hostel.repository.HostelBlockRepository;
import com.hostel.repository.LeaveRequestRepository;
import com.hostel.repository.NoticeRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;

import com.hostel.security.JwtUtils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class WardenService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(WardenService.class);

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String[] REQUIRED_CSV_HEADERS =
            {"name", "email", "enrollmentNo", "password"};

    @Autowired
    @Lazy
    private WardenService self;

    private final WardenRepository wardenRepository;
    private final UserRepository userRepository;
    private final HostelBlockRepository hostelBlockRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ComplaintRepository complaintRepository;
    private final NoticeRepository noticeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public WardenService(
            WardenRepository wardenRepository,
            UserRepository userRepository,
            HostelBlockRepository hostelBlockRepository,
            RoomRepository roomRepository,
            StudentRepository studentRepository,
            LeaveRequestRepository leaveRequestRepository,
            ComplaintRepository complaintRepository,
            NoticeRepository noticeRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtUtils jwtUtils) {

        this.wardenRepository = wardenRepository;
        this.userRepository = userRepository;
        this.hostelBlockRepository = hostelBlockRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.complaintRepository = complaintRepository;
        this.noticeRepository = noticeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    // ============================================================
    // WARDEN DASHBOARD
    // ============================================================

    public ApiResponse<DashboardStatsDto> getDashboardStats(Long wardenUserId) {

        Warden warden = wardenRepository.findByUserId(wardenUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Warden not found for userId: " + wardenUserId
                        ));

        HostelBlock block = warden.getBlock();

        // Warden has not been assigned to any block yet
        if (block == null) {

            DashboardStatsDto empty = DashboardStatsDto.builder()
                    .totalStudents(0)
                    .totalRooms(0)
                    .occupiedRooms(0)
                    .availableRooms(0)
                    .totalComplaints(0)
                    .pendingComplaints(0)
                    .resolvedComplaints(0)
                    .totalLeaves(0)
                    .pendingLeaves(0)
                    .approvedLeaves(0)
                    .build();

            return ApiResponse.success(empty);
        }

        // Get rooms belonging ONLY to this warden's block
        List<Room> blockRooms =
                roomRepository.findByBlockId(block.getId());

        long totalRooms = blockRooms.size();

        long occupiedRooms = blockRooms.stream()
                .filter(room ->
                        room.getStatus() == Room.RoomStatus.OCCUPIED)
                .count();

        long availableRooms = blockRooms.stream()
                .filter(room ->
                        room.getStatus() == Room.RoomStatus.AVAILABLE)
                .count();

        // Get students belonging to this block
        List<Student> blockStudents = new ArrayList<>();

        for (Room room : blockRooms) {
            blockStudents.addAll(
                    studentRepository.findByRoom(room)
            );
        }

        long totalStudents = blockStudents.size();

        long pendingLeaves = 0;
        long approvedLeaves = 0;

        long totalComplaints = 0;
        long pendingComplaints = 0;
        long resolvedComplaints = 0;

        for (Student student : blockStudents) {

            // Leave statistics
            List<LeaveRequest> leaves =
                    leaveRequestRepository.findByStudentId(student.getId());

            pendingLeaves += leaves.stream()
                    .filter(leave ->
                            leave.getStatus() ==
                                    LeaveRequest.LeaveStatus.PENDING)
                    .count();

            approvedLeaves += leaves.stream()
                    .filter(leave ->
                            leave.getStatus() ==
                                    LeaveRequest.LeaveStatus.APPROVED)
                    .count();

            // Complaint statistics
            List<Complaint> complaints =
                    complaintRepository.findByStudentId(student.getId());

            totalComplaints += complaints.size();

            pendingComplaints += complaints.stream()
                    .filter(complaint ->
                            complaint.getStatus() ==
                                    Complaint.ComplaintStatus.PENDING)
                    .count();

            resolvedComplaints += complaints.stream()
                    .filter(complaint ->
                            complaint.getStatus() ==
                                    Complaint.ComplaintStatus.RESOLVED)
                    .count();
        }

        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalStudents(totalStudents)
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .availableRooms(availableRooms)
                .totalComplaints(totalComplaints)
                .pendingComplaints(pendingComplaints)
                .resolvedComplaints(resolvedComplaints)
                .totalLeaves(
                        blockStudents.stream()
                                .mapToLong(student ->
                                        leaveRequestRepository
                                                .findByStudentId(student.getId())
                                                .size())
                                .sum()
                )
                .pendingLeaves(pendingLeaves)
                .approvedLeaves(approvedLeaves)
                .build();

        return ApiResponse.success(stats);
    }

    // ============================================================
    // GET ALL WARDENS
    // ADMIN ONLY
    // ============================================================

    public ApiResponse<List<Map<String, Object>>> getAllWardens() {

        List<Warden> wardens = wardenRepository.findAll();

        List<Map<String, Object>> wardenList =
                new ArrayList<>();

        for (Warden warden : wardens) {

            Map<String, Object> map = new HashMap<>();

            map.put("id", warden.getId());

            map.put(
                    "name",
                    warden.getUser() != null
                            ? warden.getUser().getName()
                            : null
            );

            map.put(
                    "email",
                    warden.getUser() != null
                            ? warden.getUser().getEmail()
                            : null
            );

            map.put(
                    "phone",
                    warden.getUser() != null
                            ? warden.getUser().getPhone()
                            : null
            );

            map.put(
                    "qualification",
                    warden.getQualification()
            );

            map.put(
                    "blockName",
                    warden.getBlock() != null
                            ? warden.getBlock().getName()
                            : null
            );

            map.put(
                    "blockId",
                    warden.getBlock() != null
                            ? warden.getBlock().getId()
                            : null
            );

            wardenList.add(map);
        }

        return ApiResponse.success(wardenList);
    }

    // ============================================================
    // ADMIN ASSIGNS WARDEN TO BLOCK
    // ============================================================

    public ApiResponse<Void> assignWardenToBlock(
            Long wardenId,
            Long blockId) {

        Warden warden = wardenRepository.findById(wardenId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Warden",
                                wardenId
                        ));

        HostelBlock block =
                hostelBlockRepository.findById(blockId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "HostelBlock",
                                        blockId
                                ));

        // Check whether another warden already owns this block
        wardenRepository.findByBlockId(blockId)
                .ifPresent(existing -> {

                    if (!existing.getId().equals(wardenId)) {

                        String name =
                                existing.getUser() != null
                                        ? existing.getUser().getName()
                                        : "another warden";

                        throw new BadRequestException(
                                "Hostel block '" +
                                        block.getName() +
                                        "' is already assigned to " +
                                        name + "."
                        );
                    }
                });

        warden.setBlock(block);

        wardenRepository.save(warden);

        return ApiResponse.success(
                "Warden assigned to block successfully",
                null
        );
    }

    // ============================================================
    // GET STUDENTS UNDER CURRENT WARDEN
    // ============================================================

    public ApiResponse<List<StudentProfileDto>>
    getStudentsByWardenBlock(Long wardenUserId) {

        Warden warden =
                wardenRepository.findByUserId(wardenUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Warden not found for userId: "
                                                + wardenUserId
                                ));

        HostelBlock block = warden.getBlock();

        // No block assigned
        if (block == null) {
            return ApiResponse.success(
                    new ArrayList<>()
            );
        }

        // Get only rooms from this block
        List<Room> blockRooms =
                roomRepository.findByBlockId(block.getId());

        List<Student> blockStudents =
                new ArrayList<>();

        // Get students from those rooms
        for (Room room : blockRooms) {

            blockStudents.addAll(
                    studentRepository.findByRoom(room)
            );
        }

        List<StudentProfileDto> dtos =
                blockStudents.stream()
                        .map(this::mapStudentToDto)
                        .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    // ============================================================
    // CREATE STUDENT ACCOUNT
    // WARDEN ONLY
    // ============================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<AuthResponse> createStudent(
            Long wardenUserId,
            RegisterRequest request) {

        // --------------------------------------------------------
        // 1. Find the logged-in warden
        // --------------------------------------------------------

        Warden warden =
                wardenRepository.findByUserId(wardenUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Warden not found for userId: "
                                                + wardenUserId
                                ));

        // --------------------------------------------------------
        // 2. Check that the warden has an assigned block
        // --------------------------------------------------------

        HostelBlock block = warden.getBlock();

        if (block == null) {

            throw new BadRequestException(
                    "You cannot create a student account because you are not assigned to a hostel block."
            );
        }

        // --------------------------------------------------------
        // 3. Check duplicate email
        // --------------------------------------------------------

        if (userRepository.existsByEmail(request.getEmail())) {

            throw new DuplicateResourceException(
                    "Email already registered: "
                            + request.getEmail()
            );
        }

        // --------------------------------------------------------
        // 4. Check enrollment number
        // --------------------------------------------------------

        if (request.getEnrollmentNo() == null ||
                request.getEnrollmentNo().isBlank()) {

            throw new BadRequestException(
                    "Enrollment number is required."
            );
        }

        if (studentRepository
                .findByEnrollmentNo(request.getEnrollmentNo())
                .isPresent()) {

            throw new DuplicateResourceException(
                    "Enrollment number already registered: "
                            + request.getEnrollmentNo()
            );
        }

        // --------------------------------------------------------
        // 5. Find an available room ONLY inside this block
        // --------------------------------------------------------

        List<Room> blockRooms =
                roomRepository.findByBlockId(block.getId());

        Room availableRoom = blockRooms.stream()
                .filter(room ->
                        room.getStatus() ==
                                Room.RoomStatus.AVAILABLE)
                .filter(room -> {

                    int occupants =
                            room.getOccupants() != null
                                    ? room.getOccupants()
                                    : 0;

                    int capacity =
                            room.getCapacity() != null
                                    ? room.getCapacity()
                                    : 0;

                    return occupants < capacity;
                })
                .findFirst()
                .orElse(null);

        // --------------------------------------------------------
        // 6. Don't create student if there is no available room
        // --------------------------------------------------------

        if (availableRoom == null) {

            throw new BadRequestException(
                    "No available room in your assigned block '"
                            + block.getName()
                            + "'. Please ask the administrator to add or free a room."
            );
        }

        // --------------------------------------------------------
        // 7. Create User account
        // --------------------------------------------------------

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(User.Role.STUDENT)
                .phone(request.getPhone())
                .build();

        user = userRepository.save(user);

        // --------------------------------------------------------
        // 8. Convert gender safely
        // --------------------------------------------------------

        Student.Gender gender = null;

        if (request.getGender() != null &&
                !request.getGender().isBlank()) {

            try {

                gender = Student.Gender.valueOf(
                        request.getGender()
                                .trim()
                                .toUpperCase()
                );

            } catch (IllegalArgumentException e) {

                throw new BadRequestException(
                        "Invalid gender. Allowed values: MALE, FEMALE, OTHER."
                );
            }
        }

        // --------------------------------------------------------
        // 9. Create Student entity
        // --------------------------------------------------------

        Student student = Student.builder()
                .user(user)
                .room(availableRoom)
                .enrollmentNo(request.getEnrollmentNo())
                .parentContact(request.getParentContact())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .gender(gender)
                .build();

        student = studentRepository.save(student);

        // --------------------------------------------------------
        // 10. Update room occupancy
        // --------------------------------------------------------

        int currentOccupants =
                availableRoom.getOccupants() != null
                        ? availableRoom.getOccupants()
                        : 0;

        availableRoom.setOccupants(
                currentOccupants + 1
        );

        int capacity =
                availableRoom.getCapacity() != null
                        ? availableRoom.getCapacity()
                        : 0;

        if (currentOccupants + 1 >= capacity) {

            availableRoom.setStatus(
                    Room.RoomStatus.OCCUPIED
            );

        } else {

            availableRoom.setStatus(
                    Room.RoomStatus.AVAILABLE
            );
        }

        roomRepository.save(availableRoom);

        // --------------------------------------------------------
        // 11. Generate JWT
        // --------------------------------------------------------

        String token =
                jwtUtils.generateToken(
                        user.getEmail(),
                        user.getRole().name()
                );

        // --------------------------------------------------------
        // 12. Build response
        // --------------------------------------------------------

        AuthResponse authResponse =
                AuthResponse.builder()
                        .token(token)
                        .role(user.getRole().name())
                        .name(user.getName())
                        .email(user.getEmail())
                        .userId(user.getId())
                        .message(
                                "Student account created successfully"
                        )
                        .build();

        return ApiResponse.success(
                "Student account created successfully",
                authResponse
        );
    }

    // ============================================================
    // BULK IMPORT STUDENTS FROM CSV
    // WARDEN ONLY
    // Each row is created in its own transaction, so one invalid
    // row never rolls back the other rows.
    // ============================================================

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApiResponse<BulkImportResultDto> bulkImportStudents(
            Long wardenUserId,
            MultipartFile file) {

        // --------------------------------------------------------
        // 1. File sanity checks
        // --------------------------------------------------------

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "Uploaded CSV file is empty."
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null ||
                !filename.toLowerCase().endsWith(".csv")) {

            throw new BadRequestException(
                    "Only CSV files are supported. Please upload a .csv file."
            );
        }

        // --------------------------------------------------------
        // 2. Parse CSV (UTF-8, BOM-safe, quoted values)
        // --------------------------------------------------------

        List<BulkImportResultDto.RowResult> results =
                new ArrayList<>();

        // Strip UTF-8 BOM (if present) BEFORE the CSV parser is
        // created, otherwise the BOM ends up on the first header.
        BufferedReader bufferedReader;

        try {

            bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    file.getInputStream(),
                                    StandardCharsets.UTF_8
                            ));

            bufferedReader.mark(1);

            if (bufferedReader.read() != '\uFEFF') {
                bufferedReader.reset();
            }

        } catch (IOException ex) {

            throw new BadRequestException(
                    "Failed to read the uploaded CSV file: " +
                            ex.getMessage()
            );
        }

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build()
                .parse(bufferedReader)) {

            Map<String, Integer> headerMap =
                    parser.getHeaderMap();

            if (headerMap == null) {
                throw new BadRequestException(
                        "CSV file has no header row."
                );
            }

            // ----------------------------------------------------
            // 3. Verify required headers
            // ----------------------------------------------------

            List<String> missingHeaders =
                    java.util.Arrays.stream(REQUIRED_CSV_HEADERS)
                            .filter(header ->
                                    !headerMap.containsKey(header))
                            .collect(Collectors.toList());

            if (!missingHeaders.isEmpty()) {

                throw new BadRequestException(
                        "CSV is missing required header(s): " +
                                String.join(", ", missingHeaders) +
                                ". Expected headers: " +
                                "name,email,enrollmentNo,password," +
                                "phone,parentContact,gender," +
                                "dateOfBirth,address"
                );
            }

            // ----------------------------------------------------
            // 4. Process each row independently
            // ----------------------------------------------------

            int rowNumber = 2; // row 1 is the header

            for (CSVRecord record : parser) {

                String name = getCell(record, "name");
                String email = getCell(record, "email");
                String enrollmentNo =
                        getCell(record, "enrollmentNo");
                String password = getCell(record, "password");
                String phone = getCell(record, "phone");
                String parentContact =
                        getCell(record, "parentContact");
                String gender = getCell(record, "gender");
                String dateOfBirth =
                        getCell(record, "dateOfBirth");
                String address = getCell(record, "address");

                // Row-level validation (backend remains authoritative;
                // createStudent re-validates everything)
                String rowError = validateRow(
                        name,
                        email,
                        enrollmentNo,
                        password,
                        gender,
                        dateOfBirth
                );

                if (rowError != null) {

                    results.add(
                            BulkImportResultDto.RowResult.builder()
                                    .rowNumber(rowNumber)
                                    .name(name)
                                    .email(email)
                                    .status("FAILED")
                                    .message(rowError)
                                    .build()
                    );

                    rowNumber++;
                    continue;
                }

                try {

                    RegisterRequest request =
                            RegisterRequest.builder()
                                    .name(name)
                                    .email(email)
                                    .password(password)
                                    .enrollmentNo(enrollmentNo)
                                    .phone(phone)
                                    .parentContact(parentContact)
                                    .gender(gender)
                                    .dateOfBirth(dateOfBirth)
                                    .address(address)
                                    .build();

                    // Runs in its own REQUIRES_NEW transaction
                    self.createStudent(wardenUserId, request);

                    // Capture the automatically assigned room
                    String roomNo = null;

                    User createdUser =
                            userRepository
                                    .findByEmail(email)
                                    .orElse(null);

                    if (createdUser != null) {

                        Optional<Student> createdStudent =
                                studentRepository
                                        .findByUser(createdUser);

                        if (createdStudent.isPresent() &&
                                createdStudent.get().getRoom() != null) {

                            roomNo = createdStudent
                                    .get()
                                    .getRoom()
                                    .getRoomNo();
                        }
                    }

                    results.add(
                            BulkImportResultDto.RowResult.builder()
                                    .rowNumber(rowNumber)
                                    .name(name)
                                    .email(email)
                                    .status("SUCCESS")
                                    .room(roomNo)
                                    .message(
                                            "Student created successfully"
                                    )
                                    .build()
                    );

                } catch (DuplicateResourceException ex) {

                    results.add(
                            BulkImportResultDto.RowResult.builder()
                                    .rowNumber(rowNumber)
                                    .name(name)
                                    .email(email)
                                    .status("FAILED")
                                    .message(ex.getMessage())
                                    .build()
                    );

                } catch (BadRequestException ex) {

                    results.add(
                            BulkImportResultDto.RowResult.builder()
                                    .rowNumber(rowNumber)
                                    .name(name)
                                    .email(email)
                                    .status("FAILED")
                                    .message(ex.getMessage())
                                    .build()
                    );

                } catch (DataIntegrityViolationException ex) {

                    results.add(
                            BulkImportResultDto.RowResult.builder()
                                    .rowNumber(rowNumber)
                                    .name(name)
                                    .email(email)
                                    .status("FAILED")
                                    .message(
                                            "Email or enrollment number already exists"
                                    )
                                    .build()
                    );

                } catch (Exception ex) {

                    log.error(
                            "Bulk import row {} failed (email: {}): {}",
                            rowNumber,
                            email,
                            ex.getMessage()
                    );

                    results.add(
                            BulkImportResultDto.RowResult.builder()
                                    .rowNumber(rowNumber)
                                    .name(name)
                                    .email(email)
                                    .status("FAILED")
                                    .message(
                                            "Unexpected error: " +
                                                    ex.getMessage()
                                    )
                                    .build()
                    );
                }

                rowNumber++;
            }

        } catch (IOException ex) {

            throw new BadRequestException(
                    "Failed to read the uploaded CSV file: " +
                            ex.getMessage()
            );
        }

        // --------------------------------------------------------
        // 5. Build summary
        // --------------------------------------------------------

        int successCount = (int) results.stream()
                .filter(result ->
                        "SUCCESS".equals(result.getStatus()))
                .count();

        int failureCount =
                results.size() - successCount;

        BulkImportResultDto summary =
                BulkImportResultDto.builder()
                        .totalRows(results.size())
                        .successCount(successCount)
                        .failureCount(failureCount)
                        .results(results)
                        .build();

        return ApiResponse.success(
                "Bulk import completed",
                summary
        );
    }

    // ============================================================
    // CSV CELL HELPER
    // ============================================================

    private String getCell(CSVRecord record, String header) {

        if (record.isMapped(header)) {
            return record.get(header);
        }

        return null;
    }

    // ============================================================
    // ROW-LEVEL VALIDATION
    // Returns an error message, or null if the row is valid.
    // ============================================================

    private String validateRow(
            String name,
            String email,
            String enrollmentNo,
            String password,
            String gender,
            String dateOfBirth) {

        if (name == null || name.isBlank()) {
            return "Name is required";
        }

        if (email == null || email.isBlank()) {
            return "Email is required";
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Invalid email format";
        }

        if (enrollmentNo == null || enrollmentNo.isBlank()) {
            return "Enrollment number is required";
        }

        if (password == null || password.isBlank()) {
            return "Password is required";
        }

        if (password.length() < 6) {
            return "Password must be at least 6 characters";
        }

        if (gender != null && !gender.isBlank()) {

            try {

                Student.Gender.valueOf(
                        gender.trim().toUpperCase()
                );

            } catch (IllegalArgumentException e) {

                return "Invalid gender. Allowed values: MALE, FEMALE, OTHER";
            }
        }

        if (dateOfBirth != null && !dateOfBirth.isBlank()) {

            try {

                LocalDate.parse(dateOfBirth.trim());

            } catch (DateTimeParseException e) {

                return "Invalid date format (expected YYYY-MM-DD)";
            }
        }

        return null;
    }

    // ============================================================
    // MAP STUDENT ENTITY TO DTO
    // ============================================================

    private StudentProfileDto mapStudentToDto(
            Student student) {

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
                .gender(
                        student.getGender() != null
                                ? student.getGender().name()
                                : null
                )
                .profileImageUrl(
                        student.getProfileImageUrl()
                )
                .roomNo(
                        student.getRoom() != null
                                ? student.getRoom().getRoomNo()
                                : null
                )
                .blockName(
                        student.getRoom() != null &&
                                student.getRoom().getBlock() != null
                                ? student.getRoom()
                                        .getBlock()
                                        .getName()
                                : null
                )
                .roomId(
                        student.getRoom() != null
                                ? student.getRoom().getId()
                                : null
                )
                .build();
    }
}
