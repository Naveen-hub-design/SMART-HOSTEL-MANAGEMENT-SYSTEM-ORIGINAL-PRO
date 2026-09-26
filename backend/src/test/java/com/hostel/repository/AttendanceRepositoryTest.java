package com.hostel.repository;

import com.hostel.entity.AttendanceRecord;
import com.hostel.entity.AttendanceStatus;
import com.hostel.entity.HostelBlock;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository-level verification on the isolated H2 test database:
 * unique constraint, block scoping and pagination.
 * Run with: mvn test -Dtest=AttendanceRepositoryTest
 */
@DataJpaTest
@ActiveProfiles("test")
class AttendanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private HostelBlock persistBlock(String name, String code) {
        HostelBlock block = HostelBlock.builder().name(name).code(code).build();
        entityManager.persist(block);
        entityManager.flush();
        return block;
    }

    private Student persistStudent(String email, HostelBlock block, String roomNo) {
        Room room = Room.builder().roomNo(roomNo).block(block)
                .floor(1).capacity(2).occupants(1)
                .status(Room.RoomStatus.AVAILABLE).build();
        entityManager.persist(room);
        User user = User.builder().name("Stu").email(email)
                .password("h").role(User.Role.STUDENT).build();
        entityManager.persist(user);
        Student student = Student.builder().user(user).room(room)
                .enrollmentNo("ENR-" + email).build();
        entityManager.persist(student);
        entityManager.flush();
        return student;
    }

    private Student persistStudent(String email, String blockName, String roomNo) {
        return persistStudent(email,
                persistBlock(blockName, "C-" + email), roomNo);
    }

    @Test
    void duplicateStudentDateRejectedByConstraint() {
        Student student = persistStudent("dup@hostel.com", "Dup", "D-1");
        LocalDate day = LocalDate.of(2026, 9, 24);

        attendanceRepository.saveAndFlush(AttendanceRecord.builder()
                .student(student).date(day)
                .status(AttendanceStatus.PRESENT).build());

        assertTrue(attendanceRepository
                .existsByStudentIdAndDate(student.getId(), day));
        assertThrows(DataIntegrityViolationException.class, () ->
                attendanceRepository.saveAndFlush(AttendanceRecord.builder()
                        .student(student).date(day)
                        .status(AttendanceStatus.ABSENT).build()));
    }

    @Test
    void blockQueryScopesAndPaginates() {
        HostelBlock mine = persistBlock("Mine", "C-MINE");
        HostelBlock theirs = persistBlock("Theirs", "C-THEIRS");
        Student ownA = persistStudent("a1@hostel.com", mine, "M-1");
        Student ownB = persistStudent("a2@hostel.com", mine, "M-2");
        Student other = persistStudent("b1@hostel.com", theirs, "T-1");
        LocalDate day = LocalDate.of(2026, 9, 24);

        attendanceRepository.save(AttendanceRecord.builder()
                .student(ownA).date(day)
                .status(AttendanceStatus.PRESENT).build());
        attendanceRepository.save(AttendanceRecord.builder()
                .student(ownB).date(day)
                .status(AttendanceStatus.ABSENT).build());
        attendanceRepository.save(AttendanceRecord.builder()
                .student(other).date(day)
                .status(AttendanceStatus.PRESENT).build());

        Long ownBlockId = ownA.getRoom().getBlock().getId();

        var page = attendanceRepository.searchByBlock(
                ownBlockId, null, null, PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());

        var filtered = attendanceRepository.searchByBlock(
                ownBlockId, day, AttendanceStatus.PRESENT,
                PageRequest.of(0, 10));
        assertEquals(1, filtered.getTotalElements());

        var secondPage = attendanceRepository.searchByBlock(
                ownBlockId, null, null, PageRequest.of(1, 1));
        assertEquals(1, secondPage.getContent().size());
        assertEquals(2, secondPage.getTotalPages());
        assertTrue(secondPage.isLast() || !secondPage.isFirst());
    }

    @Test
    void studentQueryFiltersByDateAndStatus() {
        Student student = persistStudent("c1@hostel.com", "Mine", "M-9");

        attendanceRepository.save(AttendanceRecord.builder()
                .student(student).date(LocalDate.of(2026, 9, 23))
                .status(AttendanceStatus.PRESENT).build());
        attendanceRepository.save(AttendanceRecord.builder()
                .student(student).date(LocalDate.of(2026, 9, 24))
                .status(AttendanceStatus.LATE).build());

        var byDate = attendanceRepository.searchByStudent(student.getId(),
                LocalDate.of(2026, 9, 24), null, PageRequest.of(0, 10));
        assertEquals(1, byDate.getTotalElements());

        var byStatus = attendanceRepository.searchByStudent(student.getId(),
                null, AttendanceStatus.PRESENT, PageRequest.of(0, 10));
        assertEquals(1, byStatus.getTotalElements());
        assertEquals(AttendanceStatus.PRESENT,
                byStatus.getContent().get(0).getStatus());

        assertTrue(attendanceRepository.findByStudentIdAndDate(
                student.getId(), LocalDate.of(2026, 9, 24)).isPresent());
        assertTrue(attendanceRepository.findByStudentIdAndDate(
                student.getId(), LocalDate.of(2026, 9, 25)).isEmpty());
    }
}
