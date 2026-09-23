package com.hostel.service;

import com.hostel.entity.HostelBlock;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import com.hostel.exception.BadRequestException;
import com.hostel.repository.HostelBlockRepository;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB-free tests for room capacity/status allocation rules.
 * Run with: mvn test -Dtest=RoomAllocationValidationTest
 */
@ExtendWith(MockitoExtension.class)
class RoomAllocationValidationTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HostelBlockRepository hostelBlockRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WardenRepository wardenRepository;

    @InjectMocks
    private RoomService roomService;

    private static final HostelBlock BLOCK =
            HostelBlock.builder().id(1L).name("A Wing").code("A").build();
    private static final User ADMIN =
            User.builder().id(1L).name("Admin").email("a@hostel.com")
                    .password("h").role(User.Role.ADMIN).build();

    private static Room room(Long id, int occupants, int capacity,
                             Room.RoomStatus status) {
        return Room.builder().id(id).roomNo("R-" + id).block(BLOCK)
                .floor(1).capacity(capacity).occupants(occupants)
                .status(status).rent(5000.0).build();
    }

    private static Student roomlessStudent() {
        return Student.builder().id(40L).enrollmentNo("ENR040")
                .user(User.builder().id(9L).name("Stu")
                        .email("stu@hostel.com").password("h")
                        .role(User.Role.STUDENT).build())
                .room(null).build();
    }

    private void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        ADMIN.getEmail(), null));
        lenient().when(userRepository.findByEmail(ADMIN.getEmail()))
                .thenReturn(Optional.of(ADMIN));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allocatesIntoPartiallyOccupiedRoom() {
        asAdmin();
        when(roomRepository.findById(8L))
                .thenReturn(Optional.of(room(8L, 1, 2, Room.RoomStatus.OCCUPIED)));
        when(studentRepository.findById(40L))
                .thenReturn(Optional.of(roomlessStudent()));

        assertTrue(roomService.allocateRoom(8L, 40L).isSuccess());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getOccupants());
        assertEquals(Room.RoomStatus.OCCUPIED, captor.getValue().getStatus());
    }

    @Test
    void allocateKeepsAvailableWhenSpaceRemains() {
        asAdmin();
        when(roomRepository.findById(9L))
                .thenReturn(Optional.of(room(9L, 0, 3, Room.RoomStatus.AVAILABLE)));
        when(studentRepository.findById(40L))
                .thenReturn(Optional.of(roomlessStudent()));

        assertTrue(roomService.allocateRoom(9L, 40L).isSuccess());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOccupants());
        assertEquals(Room.RoomStatus.AVAILABLE, captor.getValue().getStatus());
    }

    @Test
    void rejectsMaintenanceRoom() {
        asAdmin();
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(room(10L, 0, 2, Room.RoomStatus.MAINTENANCE)));

        assertThrows(BadRequestException.class,
                () -> roomService.allocateRoom(10L, 40L));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void rejectsFullRoom() {
        asAdmin();
        when(roomRepository.findById(11L))
                .thenReturn(Optional.of(room(11L, 2, 2, Room.RoomStatus.OCCUPIED)));

        assertThrows(BadRequestException.class,
                () -> roomService.allocateRoom(11L, 40L));
    }

    @Test
    void vacateNormalizesStaleOccupiedStatus() {
        asAdmin();
        Student st = roomlessStudent();
        Room r = room(12L, 2, 3, Room.RoomStatus.OCCUPIED);
        st.setRoom(r);
        when(studentRepository.findById(40L)).thenReturn(Optional.of(st));

        assertTrue(roomService.vacateRoom(40L).isSuccess());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getOccupants());
        assertEquals(Room.RoomStatus.AVAILABLE, captor.getValue().getStatus());
    }

    @Test
    void vacatePreservesMaintenanceStatus() {
        asAdmin();
        Student st = roomlessStudent();
        Room r = room(13L, 1, 2, Room.RoomStatus.MAINTENANCE);
        st.setRoom(r);
        when(studentRepository.findById(40L)).thenReturn(Optional.of(st));

        assertTrue(roomService.vacateRoom(40L).isSuccess());

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getOccupants());
        assertEquals(Room.RoomStatus.MAINTENANCE, captor.getValue().getStatus());
    }

    @Test
    void vacateRoomlessStudentFails() {
        asAdmin();
        when(studentRepository.findById(40L))
                .thenReturn(Optional.of(roomlessStudent()));

        assertThrows(BadRequestException.class,
                () -> roomService.vacateRoom(40L));
    }
}
