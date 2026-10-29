package com.adijayantara.cashflow.repository;

import com.adijayantara.cashflow.domain.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByNameIgnoreCaseAndDeletedAtIsNull(String name);
    Page<Customer> findByDeletedAtIsNull(Pageable pageable);
    List<Customer> findByDeletedAtIsNullOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
}

