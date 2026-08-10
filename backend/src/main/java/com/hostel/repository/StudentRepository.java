package com.hostel.repository;

import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByEnrollmentNo(String enrollmentNo);
    List<Student> findByRoom(Room room);
    List<Student> findByRoomId(Long roomId);

    @Query(value = "SELECT COUNT(*) FROM students WHERE room_id IS NOT NULL", nativeQuery = true)
    long countStudentsWithRoom();

    @Query(value = "SELECT COUNT(DISTINCT room_id) FROM students WHERE room_id IS NOT NULL", nativeQuery = true)
    long countUsedRooms();
}
