package com.clinic.service;

import com.clinic.dto.request.LoginRequest;
import com.clinic.dto.request.RegisterRequest;
import com.clinic.dto.response.AuthResponse;
import com.clinic.entity.User;
import com.clinic.entity.enums.Role;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.repository.UserRepository;
import com.clinic.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtUtil jwtUtil;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-that-is-at-least-32-bytes-long!!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);

        authService = new AuthService(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void registerNormalizesEmailToLowercase() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Doe");
        request.setEmail(" Alice.DOE@Example.COM ");
        request.setPassword("SecretPass123");

        when(userRepository.existsByEmail("alice.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("alice.doe@example.com", response.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("alice.doe@example.com", captor.getValue().getEmail());
    }

    @Test
    void registerRejectsDuplicateEmailWithConflictException() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Bob");
        request.setEmail("bob@example.com");
        request.setPassword("SecretPass123");

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsPasswordExceeding72Utf8Bytes() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Bob");
        request.setEmail("bob@example.com");
        // String of 73 characters/bytes
        request.setPassword("A1" + "a".repeat(71));

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("Alice@example.com");
        request.setPassword("CorrectPassword1");

        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("hashed-pwd")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("CorrectPassword1", "hashed-pwd")).thenReturn(true);

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("alice@example.com", response.getEmail());
    }

    @Test
    void loginWithWrongPasswordThrowsBadCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("WrongPassword");

        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("hashed-pwd")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed-pwd")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
