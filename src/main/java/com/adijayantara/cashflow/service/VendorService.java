package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Vendor;
import com.adijayantara.cashflow.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
}
