package com.adijayantara.cashflow.service;

import com.adijayantara.cashflow.domain.entity.Customer;
import com.adijayantara.cashflow.repository.CustomerRepository;
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

    @Transactional(readOnly = true)
    public Map<String, Object> listAllCustomers(int page, int limit, String search, String sortBy, String sortDir) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 50;

        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.trim().isEmpty()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), like);
                Predicate picMatch = cb.like(cb.lower(root.get("picName")), like);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phone")), like);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), like);
                predicates.add(cb.or(nameMatch, picMatch, phoneMatch, emailMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String property = "created_at".equalsIgnoreCase(sortBy) ? "createdAt" : "name";
        Sort sort = "DESC".equalsIgnoreCase(sortDir) ? Sort.by(Sort.Direction.DESC, property) : Sort.by(Sort.Direction.ASC, property);

        Page<Customer> resultPage = customerRepository.findAll(spec, PageRequest.of(page - 1, limit, sort));

        Map<String, Object> result = new HashMap<>();
        result.put("items", resultPage.getContent());
        result.put("total", resultPage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        return result;
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByID(int id) {
        return customerRepository.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new NoSuchElementException("Customer dengan ID " + id + " tidak ditemukan"));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        String cleanName = customer.getName().trim();
        if (customerRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(cleanName)) {
            throw new IllegalArgumentException("Customer dengan nama '" + cleanName + "' sudah terdaftar");
        }
        customer.setName(cleanName);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(int id, Customer updated) {
        Customer existing = getCustomerByID(id);
        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            existing.setName(updated.getName().trim());
        }
        if (updated.getPicName() != null) existing.setPicName(updated.getPicName().trim());
        if (updated.getEmail() != null) existing.setEmail(updated.getEmail().trim());
        if (updated.getPhone() != null) existing.setPhone(updated.getPhone().trim());
        if (updated.getAddress() != null) existing.setAddress(updated.getAddress().trim());
        if (updated.getNotes() != null) existing.setNotes(updated.getNotes().trim());

        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomer(int id) {
        Customer existing = getCustomerByID(id);
        existing.setDeletedAt(LocalDateTime.now());
        customerRepository.save(existing);
    }
}
