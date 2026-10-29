package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Vendor;
import com.adijayantara.cashflow.repository.VendorRepository;
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
public class VendorService {

    private final VendorRepository vendorRepository;

    @Transactional
    public Vendor findOrCreateVendor(String name, String contactInfo, UUID creatorId) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String cleanName = name.trim();

        return vendorRepository.findByNameIgnoreCaseAndDeletedAtIsNull(cleanName)
                .orElseGet(() -> {
                    log.info("Auto-registering new vendor from cashflow entry: {}", cleanName);
                    Vendor newVendor = Vendor.builder()
                            .name(cleanName)
                            .notes(contactInfo != null ? contactInfo.trim() : "")
                            .build();
                    return vendorRepository.save(newVendor);
                });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAllVendors(int page, int limit, String search, String sortBy, String sortDir) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 50;

        Specification<Vendor> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), like);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), like);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), like);
                predicates.add(cb.or(nameMatch, phoneMatch, emailMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String property = "created_at".equalsIgnoreCase(sortBy) ? "createdAt" : "name";
        Sort sort = "DESC".equalsIgnoreCase(sortDir) ? Sort.by(Sort.Direction.DESC, property) : Sort.by(Sort.Direction.ASC, property);

        Page<Vendor> resultPage = vendorRepository.findAll(spec, PageRequest.of(page - 1, limit, sort));

        Map<String, Object> result = new HashMap<>();
        result.put("items", resultPage.getContent());
        result.put("total", resultPage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }

    @Transactional(readOnly = true)
    public Vendor getVendorByID(int id) {
        return vendorRepository.findById(id)
                .filter(v -> v.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Vendor dengan ID " + id + " tidak ditemukan"));
    }

    @Transactional
    public Vendor createVendor(Vendor vendor) {
        String cleanName = vendor.getName().trim();
        if (vendorRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(cleanName)) {
            throw new IllegalArgumentException("Vendor dengan nama '" + cleanName + "' sudah terdaftar");
        }
        vendor.setName(cleanName);
        return vendorRepository.save(vendor);
    }

    @Transactional
    public Vendor updateVendor(int id, Vendor updated) {
        Vendor existing = getVendorByID(id);
        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail().trim());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone().trim());
        if (updated.getNotes() != null) existing.setNotes(updated.getNotes().trim());

        return vendorRepository.save(existing);
    }

    @Transactional
    public void deleteVendor(int id) {
        Vendor existing = getVendorByID(id);
        existing.setDeletedAt(LocalDateTime.now());
        vendorRepository.save(existing);
    }
}
