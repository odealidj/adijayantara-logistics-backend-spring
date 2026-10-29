package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.ActivityPreset;
import com.adijayantara.cashflow.repository.ActivityPresetRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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

    @Transactional(readOnly = true)
    public Map<String, Object> listPresets(String category, String search, int page, int limit, String sortBy, String sortDir) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 100;

        Specification<ActivityPreset> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (category != null && !category.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("category")), category.trim().toUpperCase()));
            }
            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), like);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), like);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String property = "created_at".equalsIgnoreCase(sortBy) ? "createdAt" : "name";
        Sort sort = "DESC".equalsIgnoreCase(sortDir) ? Sort.by(Sort.Direction.DESC, property) : Sort.by(Sort.Direction.ASC, property);

        Page<ActivityPreset> resultPage = activityPresetRepository.findAll(spec, PageRequest.of(page - 1, limit, sort));

        Map<String, Object> result = new HashMap<>();
        result.put("items", resultPage.getContent());
        result.put("total", resultPage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }

    @Transactional(readOnly = true)
    public ActivityPreset getPresetByID(int id) {
        return activityPresetRepository.findById(id)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Preset aktivitas dengan ID " + id + " tidak ditemukan"));
    }

    @Transactional
    public ActivityPreset createPreset(ActivityPreset preset) {
        String cleanCat = preset.getCategory().trim().toUpperCase();
        String cleanName = preset.getName().trim();

        if (activityPresetRepository.findByCategoryAndNameIgnoreCaseAndDeletedAtIsNull(cleanCat, cleanName).isPresent()) {
            throw new IllegalArgumentException("Preset dengan kategori '" + cleanCat + "' dan nama '" + cleanName + "' sudah ada");
        }

        preset.setCategory(cleanCat);
        preset.setName(cleanName);
        if (preset.getIsActive() == null) preset.setIsActive(true);

        return activityPresetRepository.save(preset);
    }

    @Transactional
    public ActivityPreset updatePreset(int id, ActivityPreset updated) {
        ActivityPreset existing = getPresetByID(id);

        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getCategory() != null && !updated.getCategory().trim().isEmpty()) {
            existing.setCategory(updated.getCategory().trim().toUpperCase());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription().trim());
        }
        if (updated.getIsActive() != null) {
            existing.setIsActive(updated.getIsActive());
        }

        return activityPresetRepository.save(existing);
    }

    @Transactional
    public void deletePreset(int id) {
        ActivityPreset existing = getPresetByID(id);
        existing.setDeletedAt(LocalDateTime.now());
        activityPresetRepository.save(existing);
    }
}
