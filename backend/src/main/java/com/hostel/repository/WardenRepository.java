package com.hostel.repository;

import com.hostel.entity.User;
import com.hostel.entity.Warden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardenRepository extends JpaRepository<Warden, Long> {
    Optional<Warden> findByUserId(Long userId);
    Optional<Warden> findByUser(User user);
    Optional<Warden> findByBlockId(Long blockId);
}
