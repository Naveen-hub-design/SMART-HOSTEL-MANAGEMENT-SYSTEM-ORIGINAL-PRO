package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.PasswordChangeRequest;
import com.hostel.dto.RoomDto;
import com.hostel.dto.StudentProfileDto;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ApiResponse<StudentProfileDto> getProfile(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        return ApiResponse.success(mapToProfileDto(student));
    }

    public ApiResponse<StudentProfileDto> updateProfile(Long userId, StudentProfileDto profileDto) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        User user = student.getUser();
        user.setName(profileDto.getName());
        user.setPhone(profileDto.getPhone());
        userRepository.save(user);

        student.setParentContact(profileDto.getParentContact());
        student.setAddress(profileDto.getAddress());
        student.setDateOfBirth(profileDto.getDateOfBirth());
        if (profileDto.getGender() != null) {
            student.setGender(Student.Gender.valueOf(profileDto.getGender().toUpperCase()));
        }
        student.setProfileImageUrl(profileDto.getProfileImageUrl());
        studentRepository.save(student);

        return ApiResponse.success("Profile updated successfully", mapToProfileDto(student));
    }

    public ApiResponse<RoomDto> getMyRoom(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        if (student.getRoom() == null) {
            throw new ResourceNotFoundException("No room assigned to this student");
        }

        RoomDto roomDto = RoomDto.builder()
                .id(student.getRoom().getId())
                .roomNo(student.getRoom().getRoomNo())
                .blockName(student.getRoom().getBlock().getName())
                .blockId(student.getRoom().getBlock().getId())
                .floor(student.getRoom().getFloor())
                .capacity(student.getRoom().getCapacity())
                .occupants(student.getRoom().getOccupants())
                .status(student.getRoom().getStatus().name())
                .rent(student.getRoom().getRent())
                .build();

        return ApiResponse.success(roomDto);
    }

    public ApiResponse<Void> changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.success("Password changed successfully", null);
    }

    public ApiResponse<List<StudentProfileDto>> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        List<StudentProfileDto> dtos = students.stream()
                .map(this::mapToProfileDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    private StudentProfileDto mapToProfileDto(Student student) {
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
                .roomId(student.getRoom() != null ? student.getRoom().getId() : null)
                .build();
    }
}
