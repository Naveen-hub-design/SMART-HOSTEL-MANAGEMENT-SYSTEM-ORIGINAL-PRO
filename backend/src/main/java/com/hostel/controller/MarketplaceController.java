package com.hostel.controller;

import com.hostel.dto.ApiResponse;
import com.hostel.dto.MarketplaceItemDto;
import com.hostel.entity.User;
import com.hostel.exception.ResourceNotFoundException;
import com.hostel.repository.UserRepository;
import com.hostel.service.MarketplaceService;
import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Marketplace", description = "Student marketplace for buying/selling")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;
    private final UserRepository userRepository;

    public MarketplaceController(MarketplaceService marketplaceService,
                                 UserRepository userRepository) {
        this.marketplaceService = marketplaceService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add marketplace item")
    public ResponseEntity<ApiResponse<Void>> addItem(
            @ModelAttribute MarketplaceItemDto marketplaceItemDto,
            @RequestParam(required = false) MultipartFile image) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(marketplaceService.addItem(userId, marketplaceItemDto, image));
    }

    @GetMapping
    @Operation(summary = "Get all items")
    public ResponseEntity<ApiResponse<List<MarketplaceItemDto>>> getAllItems() {
        return ResponseEntity.ok(marketplaceService.getAllItems());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    @Operation(summary = "Get my listings")
    public ResponseEntity<ApiResponse<List<MarketplaceItemDto>>> getMyItems() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(marketplaceService.getMyItems(userId));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/{id}/sold")
    @Operation(summary = "Mark item as sold")
    public ResponseEntity<ApiResponse<Void>> markAsSold(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(marketplaceService.markAsSold(id, userId));
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete marketplace item")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(marketplaceService.deleteItem(id, userId));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Search items by category")
    public ResponseEntity<ApiResponse<List<MarketplaceItemDto>>> searchByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(marketplaceService.searchByCategory(category));
    }

    private Long getCurrentUserId() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return user.getId();
    }

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
