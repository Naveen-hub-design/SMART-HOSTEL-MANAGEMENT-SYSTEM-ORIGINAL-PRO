package com.hostel.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards attendance endpoint role wiring by contract.
 * Actual 403 enforcement is additionally verified live against the
 * running backend (see attendance implementation report).
 */
class AttendanceControllerSecurityTest {

    @Test
    void controllerScopedToAttendancePath() {
        RequestMapping mapping =
                AttendanceController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/attendance"}, mapping.value());
    }

    @Test
    void staffEndpointsRequireAdminOrWarden() {
        Map<String, Integer> counts = new HashMap<>();
        for (Method method : AttendanceController.class.getDeclaredMethods()) {
            boolean mapped = method.getAnnotation(PostMapping.class) != null
                    || method.getAnnotation(PutMapping.class) != null
                    || (method.getAnnotation(GetMapping.class) != null
                            && !method.getName().equals("getMyAttendance"));
            if (!mapped) {
                continue;
            }
            PreAuthorize preAuthorize =
                    method.getAnnotation(PreAuthorize.class);
            assertNotNull(preAuthorize,
                    "Missing @PreAuthorize on " + method.getName());
            String value = preAuthorize.value();
            assertTrue(value.contains("ADMIN"),
                    "ADMIN missing on " + method.getName());
            assertTrue(value.contains("WARDEN"),
                    "WARDEN missing on " + method.getName());
            assertFalse(value.contains("STUDENT"),
                    "STUDENT must not be permitted on " + method.getName());
            counts.merge(method.getName(), 1, Integer::sum);
        }
        assertEquals(4, counts.size(),
                "Expected exactly 4 staff endpoints, found: " + counts);
    }

    @Test
    void studentEndpointIsStudentOnly() throws Exception {
        Method method = AttendanceController.class
                .getDeclaredMethod("getMyAttendance",
                        java.time.LocalDate.class,
                        com.hostel.entity.AttendanceStatus.class,
                        int.class, int.class);
        PreAuthorize preAuthorize =
                method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("STUDENT"));
        assertFalse(preAuthorize.value().contains("ADMIN"));
        assertFalse(preAuthorize.value().contains("WARDEN"));
    }
}
