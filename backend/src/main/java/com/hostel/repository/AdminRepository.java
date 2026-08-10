package com.hostel.repository;

import com.hostel.entity.Admin;
import com.hostel.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUserId(Long userId);
    Optional<Admin> findByUser(User user);
}
