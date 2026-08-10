package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.MessFeedbackDto;
import com.hostel.entity.MessFeedback;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.MessFeedbackRepository;
import com.hostel.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessFeedbackService {

    private final MessFeedbackRepository messFeedbackRepository;
    private final StudentRepository studentRepository;

    public MessFeedbackService(MessFeedbackRepository messFeedbackRepository,
                               StudentRepository studentRepository) {
        this.messFeedbackRepository = messFeedbackRepository;
        this.studentRepository = studentRepository;
    }

    public ApiResponse<Void> submitFeedback(Long userId, MessFeedbackDto feedbackDto) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        MessFeedback.Sentiment sentiment = null;
        if (feedbackDto.getSentiment() != null) {
            try {
                sentiment = MessFeedback.Sentiment.valueOf(feedbackDto.getSentiment().toUpperCase());
            } catch (IllegalArgumentException e) {
                sentiment = null;
            }
        }

        String date = feedbackDto.getDate();
        if (date == null || date.isBlank()) {
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        MessFeedback feedback = MessFeedback.builder()
                .student(student)
                .date(date)
                .foodQualityRating(feedbackDto.getFoodQualityRating())
                .tasteRating(feedbackDto.getTasteRating())
                .cleanlinessRating(feedbackDto.getCleanlinessRating())
                .comments(feedbackDto.getComments())
                .sentiment(sentiment)
                .build();
        messFeedbackRepository.save(feedback);

        return ApiResponse.success("Feedback submitted successfully", null);
    }

    public ApiResponse<List<MessFeedbackDto>> getMyFeedback(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        List<MessFeedback> feedbackList = messFeedbackRepository.findByStudentId(student.getId());
        List<MessFeedbackDto> dtos = feedbackList.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<MessFeedbackDto>> getAllFeedback() {
        List<MessFeedback> feedbackList = messFeedbackRepository.findAllByOrderByCreatedAtDesc();
        List<MessFeedbackDto> dtos = feedbackList.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Map<String, Double>> getAverageRatings() {
        Double foodQuality = messFeedbackRepository.averageFoodQualityRating();
        Double taste = messFeedbackRepository.averageTasteRating();
        Double cleanliness = messFeedbackRepository.averageCleanlinessRating();

        Double fqVal = foodQuality != null ? Math.round(foodQuality * 100.0) / 100.0 : 0.0;
        Double tasteVal = taste != null ? Math.round(taste * 100.0) / 100.0 : 0.0;
        Double cleanVal = cleanliness != null ? Math.round(cleanliness * 100.0) / 100.0 : 0.0;

        Map<String, Double> averages = new HashMap<>();
        averages.put("foodQualityRating", fqVal);
        averages.put("tasteRating", tasteVal);
        averages.put("cleanlinessRating", cleanVal);
        averages.put("foodQuality", fqVal);
        averages.put("taste", tasteVal);
        averages.put("cleanliness", cleanVal);

        return ApiResponse.success(averages);
    }

    private MessFeedbackDto mapToDto(MessFeedback feedback) {
        return MessFeedbackDto.builder()
                .id(feedback.getId())
                .studentName(feedback.getStudent().getUser().getName())
                .date(feedback.getDate())
                .foodQualityRating(feedback.getFoodQualityRating())
                .tasteRating(feedback.getTasteRating())
                .cleanlinessRating(feedback.getCleanlinessRating())
                .comments(feedback.getComments())
                .sentiment(feedback.getSentiment() != null ? feedback.getSentiment().name() : null)
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
