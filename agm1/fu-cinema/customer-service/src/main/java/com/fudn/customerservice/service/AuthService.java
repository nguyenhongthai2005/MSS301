package com.fudn.customerservice.service;

import com.fudn.customerservice.dto.LoginRequest;
import com.fudn.customerservice.dto.LoginResponse;
import com.fudn.customerservice.exception.ApiException;
import com.fudn.customerservice.model.Customer;
import com.fudn.customerservice.model.CustomerStatus;
import com.fudn.customerservice.repository.CustomerRepository;
import com.fudn.customerservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final long ADMIN_ID = 0L;

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public LoginResponse login(LoginRequest request) {
        // (1) Admin: kiem tra admin mac dinh trong application.properties HOAC tai khoan admin rut gon (admin / 123)
        boolean isOfficialAdmin = adminEmail.equalsIgnoreCase(request.email());
        boolean isShortAdmin = "admin".equalsIgnoreCase(request.email()) || "admin@gmail.com".equalsIgnoreCase(request.email());

        if (isOfficialAdmin || isShortAdmin) {
            boolean validPass = (isOfficialAdmin && adminPassword.equals(request.password()))
                    || (isShortAdmin && ("123".equals(request.password()) || adminPassword.equals(request.password())));
            if (!validPass) {
                throw ApiException.unauthorized("Invalid email or password");
            }
            return buildResponse(ADMIN_ID, adminEmail, "Administrator", ROLE_ADMIN);
        }

        // (2) Customer: tim trong DB, so khop BCrypt
        Customer customer = customerRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), customer.getPassword())) {
            throw ApiException.unauthorized("Invalid email or password");
        }
        // (3) Chan tai khoan INACTIVE (BR02)
        if (customer.getCustomerStatus() == CustomerStatus.INACTIVE) {
            throw ApiException.forbidden("Your account is inactive. Please contact the administrator.");
        }
        return buildResponse(customer.getCustomerId(), customer.getEmail(), customer.getCustomerName(), ROLE_CUSTOMER);
    }

    private LoginResponse buildResponse(Long userId, String email, String fullName, String role) {
        String token = jwtService.generateToken(userId, email, role);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), role, userId, email, fullName);
    }
}
