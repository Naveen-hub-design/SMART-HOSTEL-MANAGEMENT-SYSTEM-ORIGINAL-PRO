package com.hostel.repository;

import com.hostel.entity.MarketplaceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, Long> {
    List<MarketplaceItem> findByStatus(MarketplaceItem.ItemStatus status);
    List<MarketplaceItem> findByCategory(String category);
    List<MarketplaceItem> findAllByOrderByCreatedAtDesc();
    List<MarketplaceItem> findBySellerId(Long studentId);
    List<MarketplaceItem> findByCategoryAndStatus(String category, MarketplaceItem.ItemStatus status);
}
