package com.hostel.repository;

import com.hostel.entity.HostelBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HostelBlockRepository extends JpaRepository<HostelBlock, Long> {
    Optional<HostelBlock> findByCode(String code);
    Optional<HostelBlock> findByName(String name);
    boolean existsByCode(String code);
}
