package com.clinic.service;

import com.clinic.dto.request.LoginRequest;
import com.clinic.dto.request.RegisterRequest;
import com.clinic.dto.response.AuthResponse;
import com.clinic.entity.User;
import com.clinic.entity.enums.Role;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.repository.UserRepository;
import com.clinic.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AUTH SERVICE
 *
 * Handles user registration and login.
 *
 * @Transactional on register:
 * If userRepository.save() succeeds but something after it fails,
 * the transaction rolls back — no orphaned user records in DB.
 *
 * @Transactional(readOnly = true) on login:
 * Tells Hibernate this is a read-only operation.
 * Hibernate skips dirty checking (comparing entity state before/after).
 * Slightly better performance for read-only methods.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Compared against when the email is unknown, so login takes about the same time either way.
    private volatile String dummyHash;

    private String dummyHash() {
        if (dummyHash == null) dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
        return dummyHash;
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Validate password byte length explicitly
        if (request.getPassword() != null && request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password cannot exceed 72 bytes in UTF-8 encoding.");
        }

        // Guard clause — fail fast before doing any work
        if (userRepository.existsByEmail(normalize(request.getEmail()))) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        // Build User using Lombok @Builder — clean and readable
        User user = User.builder()
                .name(request.getName())
                .email(normalize(request.getEmail()))
                // passwordEncoder.encode() → BCrypt hash. This is one-way, cannot be reversed.
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PATIENT)
                .build();

        userRepository.save(user);
        // After save(), Hibernate flushes to DB and user.getId() is populated.

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("Registration successful")
                .build();
    }

    /** Verified identity of the caller, read from the database (not from the token). */
    @Transactional(readOnly = true)
    public AuthResponse currentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid session"));
        return AuthResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        // orElseThrow — if not found, throw exception (handled by GlobalExceptionHandler)
        User user = userRepository.findByEmail(normalize(request.getEmail())).orElse(null);

        // Same message and same work for "unknown email" and "wrong password", so the
        // response never reveals whether an account exists.
        String hash = user != null ? user.getPassword() : dummyHash();
        boolean passwordOk = passwordEncoder.matches(request.getPassword(), hash);
        if (user == null || !passwordOk) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("Login successful")
                .build();
    }
}
