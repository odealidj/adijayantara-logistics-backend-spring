package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.ActivityPreset;
import com.adijayantara.cashflow.repository.ActivityPresetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityPresetService {

    private final ActivityPresetRepository activityPresetRepository;

    @Transactional
    public ActivityPreset findOrCreatePreset(String category, String name, String description, UUID creatorId) {
        if (category == null || category.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return null;
        }
        String cleanCategory = category.trim().toUpperCase();
        String cleanName = name.trim();

        return activityPresetRepository.findByCategoryAndNameIgnoreCaseAndDeletedAtIsNull(cleanCategory, cleanName)
                .orElseGet(() -> {
                    log.info("Auto-registering activity preset [{}]: {}", cleanCategory, cleanName);
                    ActivityPreset preset = ActivityPreset.builder()
                            .category(cleanCategory)
                            .name(cleanName)
                            .description(description != null ? description.trim() : "")
                            .isActive(true)
                            .build();
                    return activityPresetRepository.save(preset);
                });
    }
}
