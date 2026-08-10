package com.hostel.service;

import com.hostel.dto.AICategorizationResponse;
import com.hostel.dto.RoomDto;
import com.hostel.entity.Room;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.RoomRepository;
import com.hostel.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new HashMap<>();

    static {
        CATEGORY_KEYWORDS.put("ELECTRICAL", Arrays.asList("electrical", "light", "fan", "switch", "power", "wiring"));
        CATEGORY_KEYWORDS.put("PLUMBING", Arrays.asList("plumbing", "pipe", "tap", "leak", "drain", "toilet"));
        CATEGORY_KEYWORDS.put("INTERNET", Arrays.asList("internet", "wifi", "network", "connection"));
        CATEGORY_KEYWORDS.put("FURNITURE", Arrays.asList("furniture", "bed", "chair", "table", "almirah", "desk"));
        CATEGORY_KEYWORDS.put("MESS", Arrays.asList("mess", "food", "canteen", "meal"));
    }

    private static final List<String> POSITIVE_KEYWORDS = Arrays.asList(
            "good", "great", "excellent", "awesome", "nice", "delicious", "tasty",
            "clean", "amazing", "wonderful", "love", "best", "fantastic", "satisfied", "happy"
    );

    private static final List<String> NEGATIVE_KEYWORDS = Arrays.asList(
            "bad", "terrible", "awful", "worst", "dirty", "disgusting", "horrible",
            "poor", "hate", "unclean", "rotten", "stale", "unsatisfied", "unhappy", "angry"
    );

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    public AIService(RoomRepository roomRepository,
                     StudentRepository studentRepository) {
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
    }

    public AICategorizationResponse categorizeComplaint(String title, String description) {
        log.info("Categorizing complaint: title={}", title);

        String text = (title + " " + description).toLowerCase();
        List<String> allMatchedKeywords = new ArrayList<>();
        String bestCategory = "GENERAL";
        int maxMatches = 0;

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            List<String> matched = new ArrayList<>();
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    matched.add(keyword);
                }
            }
            if (matched.size() > maxMatches) {
                maxMatches = matched.size();
                bestCategory = entry.getKey();
                allMatchedKeywords = matched;
            }
        }

        double confidenceScore = 0.0;
        if (!allMatchedKeywords.isEmpty()) {
            int totalKeywords = CATEGORY_KEYWORDS.getOrDefault(bestCategory, List.of()).size();
            confidenceScore = Math.min(1.0, (double) allMatchedKeywords.size() / totalKeywords);
        }

        log.info("Complaint categorized as: {} with confidence: {}", bestCategory, confidenceScore);
        return AICategorizationResponse.builder()
                .category(bestCategory)
                .confidenceScore(confidenceScore)
                .matchedKeywords(allMatchedKeywords)
                .build();
    }

    public String analyzeSentiment(String feedbackText) {
        log.info("Analyzing sentiment for feedback");

        String text = feedbackText.toLowerCase();
        int positiveCount = 0;
        int negativeCount = 0;

        for (String keyword : POSITIVE_KEYWORDS) {
            if (text.contains(keyword)) {
                positiveCount++;
            }
        }

        for (String keyword : NEGATIVE_KEYWORDS) {
            if (text.contains(keyword)) {
                negativeCount++;
            }
        }

        String sentiment;
        if (positiveCount > negativeCount) {
            sentiment = "POSITIVE";
        } else if (negativeCount > positiveCount) {
            sentiment = "NEGATIVE";
        } else {
            sentiment = "NEUTRAL";
        }

        log.info("Sentiment result: {} (positive={}, negative={})", sentiment, positiveCount, negativeCount);
        return sentiment;
    }

    public List<RoomDto> recommendRoom(Long studentUserId) {
        log.info("Recommending rooms for student userId: {}", studentUserId);

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + studentUserId));

        String enrollmentNo = student.getEnrollmentNo();
        String department = extractDepartment(enrollmentNo);
        int year = extractYear(enrollmentNo);

        List<Room> availableRooms = roomRepository.findAvailableRooms().stream()
                .filter(room -> room.getOccupants() < room.getCapacity())
                .collect(Collectors.toList());

        Map<Room, Integer> roomScores = new HashMap<>();
        for (Room room : availableRooms) {
            int score = 0;

            String blockCode = room.getBlock().getCode().toLowerCase();
            if (department != null && blockCode.contains(department.toLowerCase())) {
                score += 30;
            }

            int emptySlots = room.getCapacity() - room.getOccupants();
            score += emptySlots * 5;

            if (room.getOccupants() == 0) {
                score += 15;
            } else if (room.getOccupants() == 1) {
                score += 10;
            } else {
                score += 5;
            }

            if (year > 0 && room.getFloor() != null) {
                int floorYearMod = room.getFloor() % 10;
                int studentYearMod = year % 10;
                if (floorYearMod == studentYearMod) {
                    score += 10;
                }
            }

            roomScores.put(room, score);
        }

        List<RoomDto> recommendations = roomScores.entrySet().stream()
                .sorted(Map.Entry.<Room, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> mapToDto(entry.getKey()))
                .collect(Collectors.toList());

        log.info("Returning {} room recommendations", recommendations.size());
        return recommendations;
    }

    private String extractDepartment(String enrollmentNo) {
        if (enrollmentNo == null || enrollmentNo.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\d*([A-Za-z]+)\\d*");
        Matcher matcher = pattern.matcher(enrollmentNo);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private int extractYear(String enrollmentNo) {
        if (enrollmentNo == null || enrollmentNo.isBlank()) {
            return 0;
        }
        Pattern pattern = Pattern.compile("(\\d{4})");
        Matcher matcher = pattern.matcher(enrollmentNo);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        pattern = Pattern.compile("(\\d{2})");
        matcher = pattern.matcher(enrollmentNo);
        if (matcher.find()) {
            return Integer.parseInt("20" + matcher.group(1));
        }
        return 0;
    }

    private RoomDto mapToDto(Room room) {
        return RoomDto.builder()
                .id(room.getId())
                .roomNo(room.getRoomNo())
                .blockName(room.getBlock().getName())
                .floor(room.getFloor())
                .capacity(room.getCapacity())
                .occupants(room.getOccupants())
                .status(room.getStatus().name())
                .rent(room.getRent())
                .build();
    }
}
