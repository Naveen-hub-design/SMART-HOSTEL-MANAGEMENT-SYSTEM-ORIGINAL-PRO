package com.hostel.repository;

import com.hostel.entity.LostAndFound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostAndFoundRepository extends JpaRepository<LostAndFound, Long> {
    List<LostAndFound> findByStatus(LostAndFound.LostFoundStatus status);
    List<LostAndFound> findByCategory(String category);
    List<LostAndFound> findAllByOrderByCreatedAtDesc();
    List<LostAndFound> findByReportedById(Long studentId);
}
