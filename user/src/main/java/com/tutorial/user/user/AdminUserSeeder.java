package com.tutorial.user.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AdminUserSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final String phoneNumber;
    private final String password;

    public AdminUserSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            @Value("${app.seed.admin.phone-number:9999999999}") String phoneNumber,
            @Value("${app.seed.admin.password:change-me}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!hasSeedCommand(args)) {
            return;
        }

        User admin = userRepository.findByPhoneNumber(phoneNumber).orElseGet(() ->
                userRepository.save(new User(phoneNumber, passwordEncoder.encode(password), UserRole.ADMIN)));

        if (admin.getUserRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Configured admin phone number belongs to a non-admin user");
        }

        authService.publishUserRegisteredEvent(admin);
        System.out.println("Admin user seed event published for user " + admin.getId());
    }

    private boolean hasSeedCommand(String[] args) {
        for (String arg : args) {
            if ("--command=seed-admin".equals(arg)) {
                return true;
            }
        }
        return false;
    }
}
