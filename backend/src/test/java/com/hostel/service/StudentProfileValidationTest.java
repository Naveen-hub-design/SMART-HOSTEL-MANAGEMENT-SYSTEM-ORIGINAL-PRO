package com.hostel.service;

import com.hostel.dto.StudentProfileDto;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.exception.BadRequestException;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB-free tests for student profile enum validation.
 * Run with: mvn test -Dtest=StudentProfileValidationTest
 */
@ExtendWith(MockitoExtension.class)
class StudentProfileValidationTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentService studentService;

    private Student student() {
        User user = User.builder().id(1L).name("Stu").email("s@hostel.com")
                .password("hashed").role(User.Role.STUDENT).build();
        return Student.builder().id(2L).user(user)
                .enrollmentNo("ENR002").build();
    }

    private static StudentProfileDto dto(String gender) {
        return StudentProfileDto.builder()
                .name("Stu").phone("999").gender(gender).build();
    }

    @Test
    void rejectsInvalidGenderWith400() {
        when(studentRepository.findByUserId(1L))
                .thenReturn(Optional.of(student()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> studentService.updateProfile(1L, dto("BOGUS")));
        assertTrue(ex.getMessage().contains("Invalid gender"));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void acceptsValidGenderCaseInsensitive() {
        Student st = student();
        when(studentRepository.findByUserId(1L))
                .thenReturn(Optional.of(st));

        assertTrue(studentService.updateProfile(1L, dto("male")).isSuccess());
        assertEquals(Student.Gender.MALE, st.getGender());
    }

    @Test
    void blankGenderLeavesExistingValue() {
        Student st = student();
        when(studentRepository.findByUserId(1L))
                .thenReturn(Optional.of(st));

        assertTrue(studentService.updateProfile(1L, dto("  ")).isSuccess());
        assertNull(st.getGender());
    }
}
