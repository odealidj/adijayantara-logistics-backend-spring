package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Customer;
import com.adijayantara.cashflow.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer findOrCreateCustomer(String name, String company, String phone, String address, UUID creatorId) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String cleanName = name.trim();

        return customerRepository.findByNameIgnoreCaseAndDeletedAtIsNull(cleanName)
                .orElseGet(() -> {
                    log.info("Auto-registering new customer from cashflow entry: {}", cleanName);
                    Customer newCustomer = Customer.builder()
                            .name(cleanName)
                            .notes(company != null ? company.trim() : "")
                            .phone(phone != null ? phone.trim() : "")
                            .address(address != null ? address.trim() : "")
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }
}
