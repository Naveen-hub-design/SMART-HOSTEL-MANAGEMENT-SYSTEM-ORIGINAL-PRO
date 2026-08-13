package com.hostel;

import com.hostel.dto.ApiResponse;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.User;
import com.hostel.entity.Warden;
import com.hostel.exception.BadRequestException;
import com.hostel.repository.HostelBlockRepository;
import com.hostel.repository.UserRepository;
import com.hostel.repository.WardenRepository;
import com.hostel.service.AdminService;
import com.hostel.service.WardenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
public class WardenBlockAssignmentTest {

    @Autowired
    private WardenRepository wardenRepository;

    @Autowired
    private HostelBlockRepository hostelBlockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WardenService wardenService;

    @Autowired
    private AdminService adminService;

    private Warden warden1;
    private Warden warden2;
    private HostelBlock blockA;
    private HostelBlock blockB;

    @BeforeEach
    void setUp() {
        // Clear any existing block assignments for test setup
        for (Warden w : wardenRepository.findAll()) {
            w.setBlock(null);
            wardenRepository.save(w);
        }

        List<HostelBlock> blocks = hostelBlockRepository.findAll();
        if (blocks.size() >= 2) {
            blockA = blocks.get(0);
            blockB = blocks.get(1);
        } else {
            blockA = hostelBlockRepository.save(HostelBlock.builder().name("Test Block A").code("TEST-A").build());
            blockB = hostelBlockRepository.save(HostelBlock.builder().name("Test Block B").code("TEST-B").build());
        }

        List<Warden> wardens = wardenRepository.findAll();
        if (wardens.size() >= 2) {
            warden1 = wardens.get(0);
            warden2 = wardens.get(1);
        } else {
            User u1 = userRepository.save(User.builder().name("Test Warden 1").email("testw1@hostel.com").password("pass").role(User.Role.WARDEN).build());
            User u2 = userRepository.save(User.builder().name("Test Warden 2").email("testw2@hostel.com").password("pass").role(User.Role.WARDEN).build());
            warden1 = wardenRepository.save(Warden.builder().user(u1).qualification("M.Sc").build());
            warden2 = wardenRepository.save(Warden.builder().user(u2).qualification("Ph.D").build());
        }
    }

    @Test
    void testAssignWardenToUnassignedBlock_Success() {
        ApiResponse<Void> response = wardenService.assignWardenToBlock(warden1.getId(), blockA.getId());
        assertTrue(response.isSuccess());

        Optional<Warden> assigned = wardenRepository.findByBlockId(blockA.getId());
        assertTrue(assigned.isPresent());
        assertEquals(warden1.getId(), assigned.get().getId());
    }

    @Test
    void testAssignAnotherWardenToAlreadyAssignedBlock_RejectsWith400() {
        // Assign warden1 to blockA
        wardenService.assignWardenToBlock(warden1.getId(), blockA.getId());

        // Attempt to assign warden2 to blockA -> should throw BadRequestException
        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            wardenService.assignWardenToBlock(warden2.getId(), blockA.getId());
        });

        assertTrue(ex.getMessage().contains("already assigned"));
    }

    @Test
    void testAssignSameWardenToSameBlockAgain_SafelySucceeds() {
        wardenService.assignWardenToBlock(warden1.getId(), blockA.getId());
        ApiResponse<Void> response = wardenService.assignWardenToBlock(warden1.getId(), blockA.getId());
        assertTrue(response.isSuccess());

        Optional<Warden> assigned = wardenRepository.findByBlockId(blockA.getId());
        assertTrue(assigned.isPresent());
        assertEquals(warden1.getId(), assigned.get().getId());
    }

    @Test
    void testGetAllHostelBlocks_HandlesUnassignedAndAssignedBlocks() {
        // Clear all assignments
        for (Warden w : wardenRepository.findAll()) {
            w.setBlock(null);
            wardenRepository.save(w);
        }

        // Assign warden1 to blockA
        wardenService.assignWardenToBlock(warden1.getId(), blockA.getId());

        ApiResponse<List<Map<String, Object>>> response = adminService.getAllHostelBlocks();
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());

        // Verify blockA has wardenName set, and unassigned blocks have null wardenName
        boolean foundBlockA = false;
        for (Map<String, Object> blockMap : response.getData()) {
            if (blockA.getId().equals(blockMap.get("id"))) {
                foundBlockA = true;
                assertNotNull(blockMap.get("wardenName"));
                assertEquals(warden1.getUser().getName(), blockMap.get("wardenName"));
            }
        }
        assertTrue(foundBlockA);
    }
}
