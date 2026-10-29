package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.ActivityPreset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityPresetRepository extends JpaRepository<ActivityPreset, Integer> {
    List<ActivityPreset> findByCategoryAndIsActiveTrueAndDeletedAtIsNullOrderByNameAsc(String category);
    Page<ActivityPreset> findByCategoryAndDeletedAtIsNull(String category, Pageable pageable);
    Optional<ActivityPreset> findByCategoryAndNameIgnoreCaseAndDeletedAtIsNull(String category, String name);
}

