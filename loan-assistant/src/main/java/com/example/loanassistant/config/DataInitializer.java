package com.example.loanassistant.config;

import com.example.loanassistant.domain.Role;
import com.example.loanassistant.domain.User;
import com.example.loanassistant.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo accounts on first start so the app is runnable out of the box.
 * Demo credentials only — replace before any real deployment.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        seed("customer", "customer123", "Casey Customer", "customer@example.com", Role.CUSTOMER);
        seed("officer", "officer123", "Olivia Officer", "officer@example.com", Role.OFFICER);
        seed("admin", "admin123", "Alex Admin", "admin@example.com", Role.ADMIN);
        log.info("Seeded demo users: customer/customer123, officer/officer123, admin/admin123");
    }

    private void seed(String username, String password, String fullName, String email, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(role);
        user.setEnabled(true);
        users.save(user);
    }
}
