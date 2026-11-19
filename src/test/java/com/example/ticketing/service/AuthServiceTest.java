package com.example.ticketing.service;

import com.example.ticketing.auth.application.AuthService;
import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.auth.infrastructure.JwtService;
import com.example.ticketing.auth.interfaces.dto.RegisterRequest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("service")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks AuthService authService;

    @Test
    void register_duplicateEmail_throwsIllegalArgument() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice@test.com", "password123", "BUYER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_newEmail_returnsToken() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var mockDetails = org.springframework.security.core.userdetails.User
                .withUsername("alice@test.com").password("hashed").authorities("ROLE_BUYER").build();
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(mockDetails);
        when(jwtService.generateToken(mockDetails)).thenReturn("jwt-token");

        var response = authService.register(new RegisterRequest("alice@test.com", "password123", "BUYER"));
        assertThat(response.token()).isEqualTo("jwt-token");
    }
}
