package com.fudn.customerservice.controller;

import com.fudn.customerservice.dto.*;
import com.fudn.customerservice.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // TODO 2.6
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody RegisterRequest request) {
        return customerService.register(request);
    }

    @GetMapping("/me")
    public CustomerResponse getProfile(@RequestHeader("X-User-Id") Long customerId) {
        return customerService.getProfile(customerId);
    }

    @PutMapping("/me")
    public CustomerResponse updateProfile(@RequestHeader("X-User-Id") Long customerId,
                                          @Valid @RequestBody ProfileUpdateRequest request) {
        return customerService.updateProfile(customerId, request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestHeader("X-User-Id") Long customerId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        customerService.changePassword(customerId, request);
    }

    // ===================== ADMIN (F3) =====================

    // TODO 3.3
    @GetMapping
    public java.util.List<CustomerResponse> search(@RequestParam(required = false) String keyword) {
        return customerService.search(keyword);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable Long id) {
        return customerService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody AdminCustomerRequest request) {
        return customerService.create(request);
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody AdminCustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        customerService.delete(id);
    }
}
