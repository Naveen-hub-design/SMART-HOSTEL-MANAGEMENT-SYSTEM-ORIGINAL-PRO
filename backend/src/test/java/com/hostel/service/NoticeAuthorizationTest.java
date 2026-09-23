package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.NoticeDto;
import com.hostel.entity.Notice;
import com.hostel.entity.User;
import com.hostel.repository.NoticeRepository;
import com.hostel.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB-free authorization tests for notice creator ownership.
 * No Spring context, no database. Run with:
 * mvn test -Dtest=NoticeAuthorizationTest
 * (The full suite is NOT run here: WardenBlockAssignmentTest targets the
 * real dev database and must not execute in this environment.)
 */
@ExtendWith(MockitoExtension.class)
class NoticeAuthorizationTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NoticeService noticeService;

    private static User user(Long id, String email, String name, User.Role role) {
        return User.builder()
                .id(id).email(email).name(name)
                .password("hashed").role(role).build();
    }

    private static Notice notice(Long id, User creator) {
        return Notice.builder()
                .id(id).title("T").content("C")
                .postedBy(creator != null ? creator.getName() : "Legacy")
                .createdBy(creator)
                .targetRole(Notice.TargetRole.ALL)
                .build();
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }

    private void stubCurrentUser(User user) {
        lenient().when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static NoticeDto updateDto() {
        return NoticeDto.builder()
                .title("Updated").content("Updated content")
                .targetRole("ALL").build();
    }

    // ---- ADMIN ----

    @Test
    void adminUpdatesAnyNotice() {
        User admin = user(1L, "a@hostel.com", "Admin", User.Role.ADMIN);
        User wardenB = user(3L, "b@hostel.com", "Warden B", User.Role.WARDEN);
        authenticateAs(admin.getEmail());
        stubCurrentUser(admin);
        when(noticeRepository.findById(10L))
                .thenReturn(Optional.of(notice(10L, wardenB)));

        ApiResponse<Void> res = noticeService.updateNotice(10L, updateDto());

        assertTrue(res.isSuccess());
        verify(noticeRepository).save(any(Notice.class));
    }

    @Test
    void adminDeletesLegacyNotice() {
        User admin = user(1L, "a@hostel.com", "Admin", User.Role.ADMIN);
        authenticateAs(admin.getEmail());
        stubCurrentUser(admin);
        Notice legacy = notice(11L, null);
        when(noticeRepository.findById(11L)).thenReturn(Optional.of(legacy));

        ApiResponse<Void> res = noticeService.deleteNotice(11L);

        assertTrue(res.isSuccess());
        verify(noticeRepository).delete(legacy);
    }

    // ---- CREATION ----

    @Test
    void wardenCreatesNotice_setsServerSideCreator() {
        User wardenA = user(2L, "wa@hostel.com", "Warden A", User.Role.WARDEN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(wardenA));
        when(userRepository.findAll()).thenReturn(List.of());

        NoticeDto dto = NoticeDto.builder()
                .title("New").content("Body").targetRole("STUDENT").build();
        ApiResponse<Void> res = noticeService.createNotice(2L, dto);

        assertTrue(res.isSuccess());
        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());
        assertEquals(wardenA.getId(), captor.getValue().getCreatedBy().getId());
        assertEquals("Warden A", captor.getValue().getPostedBy());
    }

    // ---- OWNER WARDEN ----

    @Test
    void wardenUpdatesOwnNotice() {
        User wardenA = user(2L, "wa@hostel.com", "Warden A", User.Role.WARDEN);
        authenticateAs(wardenA.getEmail());
        stubCurrentUser(wardenA);
        when(noticeRepository.findById(20L))
                .thenReturn(Optional.of(notice(20L, wardenA)));

        assertTrue(noticeService.updateNotice(20L, updateDto()).isSuccess());
    }

    @Test
    void wardenDeletesOwnNotice() {
        User wardenA = user(2L, "wa@hostel.com", "Warden A", User.Role.WARDEN);
        authenticateAs(wardenA.getEmail());
        stubCurrentUser(wardenA);
        Notice owned = notice(21L, wardenA);
        when(noticeRepository.findById(21L)).thenReturn(Optional.of(owned));

        assertTrue(noticeService.deleteNotice(21L).isSuccess());
        verify(noticeRepository).delete(owned);
    }

    // ---- IDOR ----

    @Test
    void wardenCannotUpdateOthersNotice() {
        User wardenA = user(2L, "wa@hostel.com", "Warden A", User.Role.WARDEN);
        User wardenB = user(3L, "wb@hostel.com", "Warden B", User.Role.WARDEN);
        authenticateAs(wardenA.getEmail());
        stubCurrentUser(wardenA);
        when(noticeRepository.findById(30L))
                .thenReturn(Optional.of(notice(30L, wardenB)));

        assertThrows(AccessDeniedException.class,
                () -> noticeService.updateNotice(30L, updateDto()));
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void wardenCannotDeleteOthersNotice() {
        User wardenA = user(2L, "wa@hostel.com", "Warden A", User.Role.WARDEN);
        User wardenB = user(3L, "wb@hostel.com", "Warden B", User.Role.WARDEN);
        authenticateAs(wardenA.getEmail());
        stubCurrentUser(wardenA);
        when(noticeRepository.findById(31L))
                .thenReturn(Optional.of(notice(31L, wardenB)));

        assertThrows(AccessDeniedException.class,
                () -> noticeService.deleteNotice(31L));
        verify(noticeRepository, never()).delete(any(Notice.class));
    }

    // ---- LEGACY (createdBy == null) ----

    @Test
    void wardenDeniedOnLegacyNotice() {
        User wardenA = user(2L, "wa@hostel.com", "Warden A", User.Role.WARDEN);
        authenticateAs(wardenA.getEmail());
        stubCurrentUser(wardenA);
        when(noticeRepository.findById(40L))
                .thenReturn(Optional.of(notice(40L, null)));
        when(noticeRepository.findById(41L))
                .thenReturn(Optional.of(notice(41L, null)));

        assertThrows(AccessDeniedException.class,
                () -> noticeService.updateNotice(40L, updateDto()));
        assertThrows(AccessDeniedException.class,
                () -> noticeService.deleteNotice(41L));
    }

    @Test
    void adminAllowedOnLegacyNotice() {
        User admin = user(1L, "a@hostel.com", "Admin", User.Role.ADMIN);
        authenticateAs(admin.getEmail());
        stubCurrentUser(admin);
        when(noticeRepository.findById(42L))
                .thenReturn(Optional.of(notice(42L, null)));

        assertTrue(noticeService.updateNotice(42L, updateDto()).isSuccess());
    }

    // ---- STUDENT ----

    @Test
    void studentDeniedNoticeMutation() {
        User student = user(4L, "s@hostel.com", "Student", User.Role.STUDENT);
        User wardenB = user(3L, "wb@hostel.com", "Warden B", User.Role.WARDEN);
        authenticateAs(student.getEmail());
        stubCurrentUser(student);
        when(noticeRepository.findById(50L))
                .thenReturn(Optional.of(notice(50L, wardenB)));

        assertThrows(AccessDeniedException.class,
                () -> noticeService.updateNotice(50L, updateDto()));
        assertThrows(AccessDeniedException.class,
                () -> noticeService.deleteNotice(50L));
    }

    // ---- CONTRACT ----

    @Test
    void noticeDtoExposesNoCreatedBy() {
        List<String> fields = Arrays.stream(NoticeDto.class.getDeclaredFields())
                .map(f -> f.getName().toLowerCase()).toList();
        assertFalse(fields.contains("createdby"));
    }

    @Test
    void readsRemainUnchanged() {
        Notice n = notice(60L, null);
        when(noticeRepository.findAllOrderByPostedAtDesc())
                .thenReturn(List.of(n));
        when(noticeRepository.findNoticesForRole(Notice.TargetRole.STUDENT))
                .thenReturn(List.of(n));

        assertEquals(1, noticeService.getAllNotices().getData().size());
        assertEquals(1,
                noticeService.getNoticesForRole("STUDENT").getData().size());
    }
}
