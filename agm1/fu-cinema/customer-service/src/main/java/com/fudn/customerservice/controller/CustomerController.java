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
}
