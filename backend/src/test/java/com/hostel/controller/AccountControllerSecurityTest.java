package com.hostel.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards account endpoint role wiring by contract: profile and password
 * endpoints must require WARDEN or ADMIN and must never permit STUDENT,
 * while the shared profile-picture endpoint permits all three roles
 * (ownership is enforced per-user server-side).
 * Actual 403 enforcement is additionally verified live against the
 * running backend (see Phase 7B implementation report).
 */
class AccountControllerSecurityTest {

    @Test
    void controllerScopedToAccountPath() {
        RequestMapping mapping =
                AccountController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/account"}, mapping.value());
    }

    @Test
    void everyEndpointRequiresWardenOrAdmin() {
        Map<String, Integer> counts = new HashMap<>();
        for (Method method : AccountController.class.getDeclaredMethods()) {
            boolean mapped = method.getAnnotation(GetMapping.class) != null
                    || method.getAnnotation(PatchMapping.class) != null
                    || method.getAnnotation(PutMapping.class) != null;
            if (!mapped) {
                continue;
            }
            PreAuthorize preAuthorize =
                    method.getAnnotation(PreAuthorize.class);
            assertNotNull(preAuthorize,
                    "Missing @PreAuthorize on " + method.getName());
            String value = preAuthorize.value();
            assertTrue(value.contains("WARDEN"),
                    "WARDEN missing on " + method.getName());
            assertTrue(value.contains("ADMIN"),
                    "ADMIN missing on " + method.getName());
            if (!method.getName().equals("uploadProfilePicture")) {
                assertFalse(value.contains("STUDENT"),
                        "STUDENT must not be permitted on " + method.getName());
            }
            counts.merge(method.getName(), 1, Integer::sum);
        }
        assertEquals(4, counts.size(),
                "Expected exactly 4 account endpoints, found: " + counts);
    }

    @Test
    void profilePicturePermitsAllAuthenticatedRoles() throws Exception {
        Method method = AccountController.class
                .getDeclaredMethod("uploadProfilePicture",
                        org.springframework.web.multipart.MultipartFile.class);
        PreAuthorize preAuthorize =
                method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        String value = preAuthorize.value();
        assertTrue(value.contains("STUDENT"));
        assertTrue(value.contains("WARDEN"));
        assertTrue(value.contains("ADMIN"));
    }
}
