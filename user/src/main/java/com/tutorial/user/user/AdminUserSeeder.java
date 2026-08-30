package com.tutorial.user.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final String phoneNumber;
    private final String password;

    public AdminUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            @Value("${app.seed.admin.phone-number:}") String phoneNumber,
            @Value("${app.seed.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    public void seed() {
        if (phoneNumber.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Admin seed requires app.seed.admin.phone-number and app.seed.admin.password");
        }

        User admin = userRepository.findByPhoneNumber(phoneNumber).orElseGet(() ->
                userRepository.save(new User(phoneNumber, passwordEncoder.encode(password), UserRole.ADMIN)));

        if (admin.getUserRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Configured admin phone number belongs to a non-admin user");
        }

        authService.publishUserRegisteredEvent(admin);
        System.out.println("Admin user seed event published for user " + admin.getId());
    }
}
