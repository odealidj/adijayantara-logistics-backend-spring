package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    Optional<Vendor> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
    Page<Vendor> findByDeletedAtIsNull(Pageable pageable);
    List<Vendor> findByDeletedAtIsNullOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}
