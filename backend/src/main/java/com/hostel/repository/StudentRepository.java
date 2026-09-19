package com.hostel.repository;

import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT COUNT(s)
        FROM Student s
        JOIN s.room r
        JOIN r.block b
        WHERE b.id = :blockId
    """)
    long countByBlockId(@Param("blockId") Long blockId);

    @Query(value = """
        SELECT DISTINCT s FROM Student s
        JOIN s.user u
        JOIN s.room r
        JOIN r.block b
        WHERE b.id = :blockId
          AND (:search IS NULL OR :search = ''
               OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.enrollmentNo) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:gender IS NULL OR s.gender = :gender)
          AND (:roomStatus IS NULL OR r.status = :roomStatus)
        """,
        countQuery = """
        SELECT COUNT(DISTINCT s) FROM Student s
        JOIN s.user u
        JOIN s.room r
        JOIN r.block b
        WHERE b.id = :blockId
          AND (:search IS NULL OR :search = ''
               OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.enrollmentNo) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:gender IS NULL OR s.gender = :gender)
          AND (:roomStatus IS NULL OR r.status = :roomStatus)
        """)
    Page<Student> searchByBlock(@Param("blockId") Long blockId,
                                @Param("search") String search,
                                @Param("gender") Student.Gender gender,
                                @Param("roomStatus") Room.RoomStatus roomStatus,
                                Pageable pageable);
}
