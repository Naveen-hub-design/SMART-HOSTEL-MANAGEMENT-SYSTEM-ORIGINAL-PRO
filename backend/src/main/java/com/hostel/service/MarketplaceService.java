package com.hostel.service;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.MarketplaceItemDto;
import com.hostel.entity.MarketplaceItem;
import com.hostel.entity.Student;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.exception.UnauthorizedException;
import com.hostel.repository.MarketplaceItemRepository;
import com.hostel.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MarketplaceService {

    private final MarketplaceItemRepository marketplaceItemRepository;
    private final StudentRepository studentRepository;
    private final FileUploadService fileUploadService;

    public MarketplaceService(MarketplaceItemRepository marketplaceItemRepository,
                              StudentRepository studentRepository,
                              FileUploadService fileUploadService) {
        this.marketplaceItemRepository = marketplaceItemRepository;
        this.studentRepository = studentRepository;
        this.fileUploadService = fileUploadService;
    }

    public ApiResponse<Void> addItem(Long userId, MarketplaceItemDto dto, MultipartFile image) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileUploadService.uploadFile(image);
        }

        MarketplaceItem item = MarketplaceItem.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .imageUrl(imageUrl)
                .category(dto.getCategory())
                .seller(student)
                .status(MarketplaceItem.ItemStatus.AVAILABLE)
                .build();
        marketplaceItemRepository.save(item);

        return ApiResponse.success("Item added successfully", null);
    }

    public ApiResponse<List<MarketplaceItemDto>> getAllItems() {
        List<MarketplaceItem> items = marketplaceItemRepository.findAllByOrderByCreatedAtDesc();
        List<MarketplaceItemDto> dtos = items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<List<MarketplaceItemDto>> getMyItems(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        List<MarketplaceItem> items = marketplaceItemRepository.findBySellerId(student.getId());
        List<MarketplaceItemDto> dtos = items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    public ApiResponse<Void> markAsSold(Long itemId, Long userId) {
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceItem", itemId));

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        if (!item.getSeller().getId().equals(student.getId())) {
            throw new UnauthorizedException("You can only mark your own items as sold");
        }

        item.setStatus(MarketplaceItem.ItemStatus.SOLD);
        marketplaceItemRepository.save(item);

        return ApiResponse.success("Item marked as sold", null);
    }

    public ApiResponse<Void> deleteItem(Long itemId, Long userId) {
        MarketplaceItem item = marketplaceItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketplaceItem", itemId));

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for userId: " + userId));

        if (!item.getSeller().getId().equals(student.getId())) {
            throw new UnauthorizedException("You can only delete your own items");
        }

        marketplaceItemRepository.delete(item);

        return ApiResponse.success("Item deleted successfully", null);
    }

    public ApiResponse<List<MarketplaceItemDto>> searchByCategory(String category) {
        List<MarketplaceItem> items = marketplaceItemRepository.findByCategoryAndStatus(category, MarketplaceItem.ItemStatus.AVAILABLE);
        List<MarketplaceItemDto> dtos = items.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    private MarketplaceItemDto mapToDto(MarketplaceItem item) {
        return MarketplaceItemDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .category(item.getCategory())
                .sellerName(item.getSeller() != null ? item.getSeller().getUser().getName() : null)
                .status(item.getStatus().name())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
