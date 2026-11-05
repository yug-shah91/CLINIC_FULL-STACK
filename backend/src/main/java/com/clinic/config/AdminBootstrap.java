package com.clinic.config;

import com.clinic.entity.User;
import com.clinic.entity.enums.Role;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first doctor (ADMIN) account from ADMIN_EMAIL / ADMIN_PASSWORD, but only when
 * no admin exists. This replaces the old "choose Doctor on the register form" shortcut.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String email;
    @Value("${app.admin.password:}")
    private String password;
    @Value("${app.admin.name:Doctor}")
    private String name;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) return;
        if (userRepository.existsByRole(Role.ADMIN)) return;
        if (password.length() < 12) {
            log.warn("ADMIN_PASSWORD is shorter than 12 characters; admin account NOT created.");
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            log.warn("ADMIN_EMAIL belongs to an existing non-admin user; admin account NOT created.");
            return;
        }
        userRepository.save(User.builder()
                .name(name)
                .email(normalized)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build());
        log.info("Created initial admin account for {}", normalized);
    }
}
