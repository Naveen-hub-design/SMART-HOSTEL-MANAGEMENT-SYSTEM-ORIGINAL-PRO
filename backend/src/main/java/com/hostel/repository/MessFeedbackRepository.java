package com.hostel.repository;

import com.hostel.entity.MessFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessFeedbackRepository extends JpaRepository<MessFeedback, Long> {
    List<MessFeedback> findByStudentId(Long studentId);

    @Query("SELECT AVG(m.foodQualityRating) FROM MessFeedback m")
    Double averageFoodQualityRating();

    @Query("SELECT AVG(m.tasteRating) FROM MessFeedback m")
    Double averageTasteRating();

    @Query("SELECT AVG(m.cleanlinessRating) FROM MessFeedback m")
    Double averageCleanlinessRating();

    List<MessFeedback> findAllByOrderByCreatedAtDesc();
}
