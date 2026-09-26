package com.hostel.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards report endpoint role wiring by contract: every report endpoint
 * must require ADMIN or WARDEN and must never permit STUDENT.
 * Actual 403 enforcement is additionally verified live against the
 * running backend (see Phase 7A implementation report).
 */
class ReportControllerSecurityTest {

    @Test
    void controllerScopedToReportsPath() {
        RequestMapping mapping =
                ReportController.class.getAnnotation(RequestMapping.class);
        assertNotNull(mapping);
        assertArrayEquals(new String[]{"/api/reports"}, mapping.value());
    }

    @Test
    void everyEndpointRequiresAdminOrWarden() {
        int checked = 0;
        for (Method method : ReportController.class.getDeclaredMethods()) {
            if (method.getAnnotation(GetMapping.class) == null) {
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
            checked++;
        }
        assertEquals(12, checked,
                "Expected exactly 12 report endpoints");
    }
}
