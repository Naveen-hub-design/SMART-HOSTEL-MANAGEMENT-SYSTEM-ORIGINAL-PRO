package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.LostAndFoundDto;
import com.hostel.entity.LostAndFound;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.LostAndFoundRepository;
import com.hostel.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LostAndFoundService {

    private final LostAndFoundRepository lostAndFoundRepository;
    private final StudentRepository studentRepository;
    private final FileUploadService fileUploadService;

    public LostAndFoundService(LostAndFoundRepository lostAndFoundRepository,
                               StudentRepository studentRepository,
                               FileUploadService fileUploadService) {
        this.lostAndFoundRepository = lostAndFoundRepository;
        this.studentRepository = studentRepository;
        this.fileUploadService = fileUploadService;
    }

    public ApiResponse<Void> reportItem(Long userId, LostAndFoundDto dto, MultipartFile image) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileUploadService.uploadFile(image);
        }

        LostAndFound.LostFoundStatus status = LostAndFound.LostFoundStatus.LOST;
        if (dto.getStatus() != null) {
            try {
                status = LostAndFound.LostFoundStatus.valueOf(dto.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                status = LostAndFound.LostFoundStatus.LOST;
            }
        }

        LostAndFound item = LostAndFound.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageUrl(imageUrl)
                .status(status)
                .category(dto.getCategory())
                .location(dto.getLocation())
                .contactInfo(dto.getContactInfo())
                .reportedBy(student)
                .build();

        lostAndFoundRepository.save(item);

        return ApiResponse.success("Item reported successfully", null);
    }

    public ApiResponse<List<LostAndFoundDto>> getAllItems() {
        List<LostAndFound> items = lostAndFoundRepository.findAllByOrderByCreatedAtDesc();
        List<LostAndFoundDto> dtos = items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<LostAndFoundDto>> getItemsByStatus(String status) {
        LostAndFound.LostFoundStatus ls = LostAndFound.LostFoundStatus.valueOf(status.toUpperCase());
        List<LostAndFound> items = lostAndFoundRepository.findByStatus(ls);
        List<LostAndFoundDto> dtos = items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Void> updateItemStatus(Long itemId, String status) {
        LostAndFound item = lostAndFoundRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("LostAndFound", itemId));

        LostAndFound.LostFoundStatus newStatus = LostAndFound.LostFoundStatus.valueOf(status.toUpperCase());
        item.setStatus(newStatus);
        lostAndFoundRepository.save(item);

        return ApiResponse.success("Item status updated successfully", null);
    }

    public ApiResponse<List<LostAndFoundDto>> getMyReports(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        List<LostAndFound> items = lostAndFoundRepository.findByReportedById(student.getId());
        List<LostAndFoundDto> dtos = items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    private LostAndFoundDto mapToDto(LostAndFound item) {
        return LostAndFoundDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .imageUrl(item.getImageUrl())
                .status(item.getStatus().name())
                .category(item.getCategory())
                .location(item.getLocation())
                .contactInfo(item.getContactInfo())
                .reporterName(item.getReportedBy() != null ? item.getReportedBy().getUser().getName() : null)
                .createdAt(item.getCreatedAt())
                .build();
    }
}
