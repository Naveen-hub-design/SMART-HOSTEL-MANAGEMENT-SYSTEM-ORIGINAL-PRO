package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.AuthResponse;
import com.hostel.dto.LoginRequest;
import com.hostel.dto.RegisterRequest;
import com.hostel.entity.Admin;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.DuplicateResourceException;
import com.hostel.repository.AdminRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import com.hostel.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final WardenRepository wardenRepository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       StudentRepository studentRepository,
                       AdminRepository adminRepository,
                       WardenRepository wardenRepository,
                       JwtUtils jwtUtils,
                       BCryptPasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.adminRepository = adminRepository;
        this.wardenRepository = wardenRepository;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public ApiResponse<AuthResponse> registerStudent(RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        // SECURITY: public registration ALWAYS creates a STUDENT.
        // Any client-supplied role value is deliberately ignored.
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.STUDENT)
                .phone(request.getPhone())
                .build();
        user = userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .enrollmentNo(request.getEnrollmentNo())
                .parentContact(request.getParentContact())
                .address(request.getAddress())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender() != null ? Student.Gender.valueOf(request.getGender().toUpperCase()) : null)
                .build();
        studentRepository.save(student);

        String token = jwtUtils.generateToken(user.getEmail(), User.Role.STUDENT.name());

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .role(User.Role.STUDENT.name())
                .name(user.getName())
                .email(user.getEmail())
                .userId(user.getId())
                .message("Registration successful")
                .build();

        log.info("User registered successfully: {} as {}", user.getEmail(), user.getRole());
        auditService.logAction("USER_REGISTERED", user.getEmail(), User.Role.STUDENT.name(), "USER", user.getId(),
                "User " + user.getEmail() + " registered as " + User.Role.STUDENT);
        return ApiResponse.success("Registration successful", authResponse);
    }

    public ApiResponse<AuthResponse> login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .name(user.getName())
                .email(user.getEmail())
                .userId(user.getId())
                .message("Login successful")
                .build();

        log.info("User logged in successfully: {}", user.getEmail());
        auditService.logAction("USER_LOGGED_IN", user.getEmail(), user.getRole().name(), "USER", user.getId(),
                "User " + user.getEmail() + " logged in");
        return ApiResponse.success("Login successful", authResponse);
    }
}
