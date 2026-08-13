package com.hostel.repository;

import com.hostel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByStatus(Room.RoomStatus status);
    List<Room> findByBlockId(Long blockId);
    long countByStatus(Room.RoomStatus status);
    Optional<Room> findByRoomNoAndBlockId(String roomNo, Long blockId);

    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE'")
    List<Room> findAvailableRooms();

    @Query("SELECT COUNT(r) FROM Room r WHERE r.status = 'OCCUPIED'")
    long countOccupiedRooms();

    @Query("SELECT COUNT(r) FROM Room r WHERE r.status = 'AVAILABLE'")
    long countAvailableRooms();

    List<Room> findByStatusAndBlockId(Room.RoomStatus status, Long blockId);

    long countByBlockId(Long blockId);

    long countByBlockIdAndStatus(Long blockId, Room.RoomStatus status);

    long countByRentIsNotNull();

    @Query("SELECT AVG(r.rent) FROM Room r")
    Double findAverageRent();
}
