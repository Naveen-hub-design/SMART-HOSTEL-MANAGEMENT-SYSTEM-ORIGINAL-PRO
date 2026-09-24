package com.hostel.service;

import com.hostel.dto.NoticeDto;
import com.hostel.entity.Notice;
import com.hostel.entity.User;
import com.hostel.exception.BadRequestException;
import com.hostel.repository.NoticeRepository;
import com.hostel.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB-free tests for notice expiry input and visibility filtering.
 * Run with: mvn test -Dtest=NoticeExpiryTest
 */
@ExtendWith(MockitoExtension.class)
class NoticeExpiryTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NoticeService noticeService;

    private static final User ADMIN =
            User.builder().id(1L).name("Admin").email("a@hostel.com")
                    .password("h").role(User.Role.ADMIN).build();

    private static Notice notice(Long id, LocalDateTime expiresAt) {
        return Notice.builder().id(id).title("T").content("C")
                .postedBy("Admin").expiresAt(expiresAt)
                .targetRole(Notice.TargetRole.ALL).build();
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
    void nullExpiryVisible() {
        when(noticeRepository.findNoticesForRole(Notice.TargetRole.STUDENT))
                .thenReturn(List.of(notice(1L, null)));

        var dtos = noticeService.getNoticesForRole("STUDENT").getData();

        assertEquals(1, dtos.size());
    }

    @Test
    void futureExpiryVisible() {
        when(noticeRepository.findNoticesForRole(Notice.TargetRole.STUDENT))
                .thenReturn(List.of(
                        notice(1L, LocalDateTime.now().plusDays(30))));

        assertEquals(1,
                noticeService.getNoticesForRole("STUDENT").getData().size());
    }

    @Test
    void pastExpiryHidden() {
        when(noticeRepository.findNoticesForRole(Notice.TargetRole.STUDENT))
                .thenReturn(List.of(
                        notice(1L, LocalDateTime.now().minusDays(1)),
                        notice(2L, null),
                        notice(3L, LocalDateTime.now().plusDays(1))));

        var dtos = noticeService.getNoticesForRole("STUDENT").getData();

        assertEquals(2, dtos.size());
        assertTrue(dtos.stream().noneMatch(d -> d.getId().equals(1L)));
    }

    @Test
    void nearBoundaryBehavesCorrectly() {
        when(noticeRepository.findNoticesForRole(Notice.TargetRole.STUDENT))
                .thenReturn(List.of(
                        notice(1L, LocalDateTime.now().plusSeconds(120)),
                        notice(2L, LocalDateTime.now().minusSeconds(120))));

        var dtos = noticeService.getNoticesForRole("STUDENT").getData();

        assertEquals(1, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
    }

    @Test
    void dateOnlyInputConvertsToEndOfDay() {
        asAdmin();
        when(userRepository.findById(1L)).thenReturn(Optional.of(ADMIN));
        when(userRepository.findAll()).thenReturn(List.of());

        NoticeDto dto = NoticeDto.builder().title("T").content("C")
                .targetRole("ALL").expiryDate("2026-09-30").build();
        assertTrue(noticeService.createNotice(1L, dto).isSuccess());

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());
        assertEquals(LocalDateTime.of(2026, 9, 30, 23, 59, 59),
                captor.getValue().getExpiresAt());
    }

    @Test
    void invalidDateInputRejected() {
        asAdmin();
        when(userRepository.findById(1L)).thenReturn(Optional.of(ADMIN));

        NoticeDto dto = NoticeDto.builder().title("T").content("C")
                .targetRole("ALL").expiryDate("not-a-date").build();
        assertThrows(BadRequestException.class,
                () -> noticeService.createNotice(1L, dto));
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    @Test
    void blankExpiryMeansNonExpiring() {
        asAdmin();
        when(userRepository.findById(1L)).thenReturn(Optional.of(ADMIN));
        when(userRepository.findAll()).thenReturn(List.of());

        NoticeDto dto = NoticeDto.builder().title("T").content("C")
                .targetRole("ALL").expiryDate("  ").build();
        assertTrue(noticeService.createNotice(1L, dto).isSuccess());

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());
        assertNull(captor.getValue().getExpiresAt());
    }

    @Test
    void updateCanChangeAndClearExpiry() {
        asAdmin();
        Notice existing = notice(5L, LocalDateTime.now().plusDays(10));
        when(noticeRepository.findById(5L)).thenReturn(Optional.of(existing));

        NoticeDto dto = NoticeDto.builder().title("T").content("C")
                .targetRole("ALL").expiryDate("2026-10-05").build();
        assertTrue(noticeService.updateNotice(5L, dto).isSuccess());
        assertEquals(LocalDateTime.of(2026, 10, 5, 23, 59, 59),
                existing.getExpiresAt());

        NoticeDto clear = NoticeDto.builder().title("T").content("C")
                .targetRole("ALL").expiryDate("").build();
        assertTrue(noticeService.updateNotice(5L, clear).isSuccess());
        assertNull(existing.getExpiresAt());
    }

    @Test
    void managementListIncludesExpired() {
        when(noticeRepository.findAllOrderByPostedAtDesc()).thenReturn(List.of(
                notice(1L, LocalDateTime.now().minusDays(5)),
                notice(2L, null)));

        assertEquals(2, noticeService.getAllNotices().getData().size());
    }
}
