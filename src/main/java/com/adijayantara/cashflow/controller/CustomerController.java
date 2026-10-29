package com.adijayantara.cashflow.controller;

import com.adijayantara.cashflow.domain.entity.Customer;
import com.adijayantara.cashflow.dto.ApiResponse;
import com.adijayantara.cashflow.dto.PaginationMeta;
import com.adijayantara.cashflow.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Endpoint master data customer / klien pengirim muatan")
@SecurityRequirement(name = "BearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('*', 'customers.view')")
    @Operation(summary = "Daftar Customer", description = "Menampilkan data customer/klien aktif dengan paginasi dan pencarian")
    public ResponseEntity<ApiResponse<List<Customer>>> list(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort_by", defaultValue = "name") String sortBy,
            @RequestParam(value = "sort_dir", defaultValue = "ASC") String sortDir
    ) {
        Map<String, Object> data = customerService.listAllCustomers(page, limit, search, sortBy, sortDir);

        @SuppressWarnings("unchecked")
        List<Customer> items = (List<Customer>) data.get("items");
        long total = (long) data.get("total");

        PaginationMeta meta = PaginationMeta.of(page, limit, (int) total);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved", items, meta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'customers.view')")
    @Operation(summary = "Detail Customer", description = "Mengambil data customer berdasarkan ID")
    public ResponseEntity<ApiResponse<Customer>> get(@PathVariable("id") int id) {
        Customer customer = customerService.getCustomerByID(id);
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", customer));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('*', 'customers.manage')")
    @Operation(summary = "Tambah Customer Baru", description = "Mendaftarkan klien muatan baru")
    public ResponseEntity<ApiResponse<Customer>> create(@RequestBody Customer customer) {
        Customer saved = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'customers.manage')")
    @Operation(summary = "Perbarui Customer", description = "Memperbarui data kontak dan PIC customer")
    public ResponseEntity<ApiResponse<Customer>> update(@PathVariable("id") int id, @RequestBody Customer customer) {
        Customer updated = customerService.updateCustomer(id, customer);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'customers.manage')")
    @Operation(summary = "Hapus Customer", description = "Soft delete customer dari master data")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") int id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }
}
