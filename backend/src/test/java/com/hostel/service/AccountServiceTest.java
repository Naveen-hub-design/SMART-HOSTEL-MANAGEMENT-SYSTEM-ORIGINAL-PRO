package com.hostel.service;

import com.hostel.dto.AccountProfileDto;
import com.hostel.dto.PasswordChangeRequest;
import com.hostel.entity.Admin;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.AdminRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DB-free tests for warden/admin self-service account management.
 * Run with: mvn test -Dtest=AccountServiceTest
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WardenRepository wardenRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private AccountService accountService;

    private static final HostelBlock BLOCK =
            HostelBlock.builder().id(1L).name("A Wing").code("A").build();

    private static User wardenUser() {
        return User.builder().id(2L).name("Warden").email("w@hostel.com")
                .password("HASHED").phone("111").role(User.Role.WARDEN).build();
    }

    private static User adminUser() {
        return User.builder().id(1L).name("Admin").email("a@hostel.com")
                .password("HASHED").phone("222").role(User.Role.ADMIN).build();
    }

    private static User studentUser() {
        return User.builder().id(4L).name("Stu").email("s@hostel.com")
                .password("HASHED").role(User.Role.STUDENT).build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null));
        lenient().when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
    }

    private void stubWardenRow(User user) {
        lenient().when(wardenRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(Warden.builder().id(9L).user(user)
                        .block(BLOCK).qualification("M.Sc").build()));
    }

    private void stubAdminRow(User user) {
        lenient().when(adminRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(Admin.builder().id(5L).user(user)
                        .department("Housing").build()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void wardenProfileGetReturnsSafeFields() {
        User warden = wardenUser();
        authenticateAs(warden);
        stubWardenRow(warden);

        AccountProfileDto dto = accountService.getProfile().getData();

        assertEquals(2L, dto.getId());
        assertEquals("Warden", dto.getName());
        assertEquals("w@hostel.com", dto.getEmail());
        assertEquals("WARDEN", dto.getRole());
        assertEquals("M.Sc", dto.getQualification());
        assertEquals("A Wing", dto.getBlockName());
        assertNull(dto.getDepartment());
    }

    @Test
    void adminProfileGetReturnsSafeFields() {
        User admin = adminUser();
        authenticateAs(admin);
        stubAdminRow(admin);

        AccountProfileDto dto = accountService.getProfile().getData();

        assertEquals("ADMIN", dto.getRole());
        assertEquals("Housing", dto.getDepartment());
        assertNull(dto.getBlockName());
        assertNull(dto.getQualification());
    }

    @Test
    void passwordHashNeverExposed() {
        List<String> fields = Arrays.stream(
                AccountProfileDto.class.getDeclaredFields())
                .map(f -> f.getName().toLowerCase()).toList();
        assertFalse(fields.contains("password"));
        assertFalse(fields.contains("passwordhash"));
        assertFalse(fields.contains("token"));
        assertFalse(fields.contains("jwt"));
    }

    @Test
    void wardenCanUpdateOwnNameAndPhone() {
        User warden = wardenUser();
        authenticateAs(warden);
        stubWardenRow(warden);

        AccountProfileDto req = AccountProfileDto.builder()
                .name("New Name").phone("999").build();
        AccountProfileDto dto =
                accountService.updateProfile(req).getData();

        assertEquals("New Name", dto.getName());
        assertEquals("999", dto.getPhone());
        verify(userRepository).save(warden);
    }

    @Test
    void adminCanUpdateOwnNameAndPhone() {
        User admin = adminUser();
        authenticateAs(admin);
        stubAdminRow(admin);

        AccountProfileDto req = AccountProfileDto.builder()
                .name("Root Two").phone("888").build();

        assertTrue(accountService.updateProfile(req).isSuccess());
        assertEquals("Root Two", admin.getName());
    }

    @Test
    void emailRoleAndRelationsCannotChange() {
        User warden = wardenUser();
        authenticateAs(warden);
        stubWardenRow(warden);

        AccountProfileDto req = AccountProfileDto.builder()
                .id(999L).name("X").email("evil@x.com").role("ADMIN")
                .qualification("PhD").department("Y").blockName("Z").build();
        accountService.updateProfile(req);

        assertEquals(2L, warden.getId());
        assertEquals("w@hostel.com", warden.getEmail());
        assertEquals(User.Role.WARDEN, warden.getRole());
    }

    @Test
    void updateResolvesCallerFromJwtNotInput() {
        User warden = wardenUser();
        authenticateAs(warden);
        stubWardenRow(warden);

        accountService.updateProfile(AccountProfileDto.builder()
                .name("X").build());

        verify(userRepository).findByEmail("w@hostel.com");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void correctCurrentPasswordSucceeds() {
        User warden = wardenUser();
        authenticateAs(warden);
        when(passwordEncoder.matches("old12345", "HASHED")).thenReturn(true);
        when(passwordEncoder.encode("new12345")).thenReturn("NEWHASH");

        assertTrue(accountService.changePassword(PasswordChangeRequest.builder()
                .currentPassword("old12345").newPassword("new12345").build())
                .isSuccess());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("NEWHASH", captor.getValue().getPassword());
    }

    @Test
    void wrongCurrentPasswordFails() {
        User warden = wardenUser();
        authenticateAs(warden);
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> accountService.changePassword(PasswordChangeRequest
                        .builder().currentPassword("wrong")
                        .newPassword("new12345").build()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shortNewPasswordFails() {
        User warden = wardenUser();
        authenticateAs(warden);
        when(passwordEncoder.matches("old12345", "HASHED")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> accountService.changePassword(PasswordChangeRequest
                        .builder().currentPassword("old12345")
                        .newPassword("123").build()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void unknownEmailReturns404() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "ghost@hostel.com", null));
        when(userRepository.findByEmail("ghost@hostel.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> accountService.getProfile());
    }

    @Test
    void studentRoleRejectedEverywhere() {
        User student = studentUser();
        authenticateAs(student);

        assertThrows(AccessDeniedException.class,
                () -> accountService.getProfile());
        assertThrows(AccessDeniedException.class,
                () -> accountService.updateProfile(
                        AccountProfileDto.builder().name("X").build()));
        assertThrows(AccessDeniedException.class,
                () -> accountService.changePassword(PasswordChangeRequest
                        .builder().currentPassword("x")
                        .newPassword("new12345").build()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void profileDtoCarriesImageUrl() {
        User warden = wardenUser();
        warden.setProfileImageUrl("/uploads/abc.png");
        authenticateAs(warden);
        stubWardenRow(warden);

        assertEquals("/uploads/abc.png",
                accountService.getProfile().getData().getProfileImageUrl());
    }

    @Test
    void wardenUploadStoresUrlOnOwnRow() throws Exception {
        User warden = wardenUser();
        authenticateAs(warden);
        org.springframework.web.multipart.MultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "pic.png", "image/png",
                        new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        when(fileUploadService.uploadFile(file))
                .thenReturn("/uploads/uuid-1.png");

        var res = accountService.uploadProfilePicture(file);

        assertTrue(res.isSuccess());
        assertEquals("/uploads/uuid-1.png", res.getData());
        assertEquals("/uploads/uuid-1.png", warden.getProfileImageUrl());
        verify(userRepository).save(warden);
    }

    @Test
    void studentUploadStoresUrlOnOwnRow() throws Exception {
        User student = studentUser();
        authenticateAs(student);
        org.springframework.web.multipart.MultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "pic.jpg", "image/jpeg",
                        new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        when(fileUploadService.uploadFile(file))
                .thenReturn("/uploads/uuid-2.jpg");

        assertTrue(accountService.uploadProfilePicture(file).isSuccess());
        assertEquals("/uploads/uuid-2.jpg", student.getProfileImageUrl());
    }

    @Test
    void adminUploadStoresUrlOnOwnRow() throws Exception {
        User admin = adminUser();
        authenticateAs(admin);
        org.springframework.web.multipart.MultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "pic.gif", "image/gif", new byte[]{0x47});
        when(fileUploadService.uploadFile(file))
                .thenReturn("/uploads/uuid-3.gif");

        assertTrue(accountService.uploadProfilePicture(file).isSuccess());
        assertEquals("/uploads/uuid-3.gif", admin.getProfileImageUrl());
    }

    @Test
    void uploadResolvesCallerFromJwtOnly() throws Exception {
        User wardenA = wardenUser();
        User wardenB = User.builder().id(3L).name("Other")
                .email("other@hostel.com").password("H")
                .role(User.Role.WARDEN).build();
        authenticateAs(wardenA);
        org.springframework.web.multipart.MultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "pic.png", "image/png", new byte[]{1, 2, 3});
        when(fileUploadService.uploadFile(file))
                .thenReturn("/uploads/uuid-4.png");

        accountService.uploadProfilePicture(file);

        // Authenticated user resolved from JWT; nothing can target warden B.
        verify(userRepository).findByEmail("w@hostel.com");
        assertEquals("/uploads/uuid-4.png", wardenA.getProfileImageUrl());
        assertNull(wardenB.getProfileImageUrl());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void uploadValidationFailurePropagates() throws Exception {
        User warden = wardenUser();
        authenticateAs(warden);
        org.springframework.web.multipart.MultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "evil.html", "text/html", new byte[]{1});
        when(fileUploadService.uploadFile(file)).thenThrow(
                new com.hostel.exception.BadRequestException("Only JPG, PNG and GIF images are allowed"));

        assertThrows(com.hostel.exception.BadRequestException.class,
                () -> accountService.uploadProfilePicture(file));
        assertNull(warden.getProfileImageUrl());
        verify(userRepository, never()).save(any(User.class));
    }
}
