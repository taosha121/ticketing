package com.example.ticketing.auth.application;

import com.example.ticketing.auth.domain.User;
import com.example.ticketing.auth.domain.UserRepository;
import com.example.ticketing.auth.infrastructure.JwtService;
import com.example.ticketing.auth.interfaces.dto.AuthResponse;
import com.example.ticketing.auth.interfaces.dto.LoginRequest;
import com.example.ticketing.auth.interfaces.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User.Role role = "ORGANIZER".equalsIgnoreCase(req.role()) ? User.Role.ORGANIZER : User.Role.BUYER;
        User user = new User(req.email(), passwordEncoder.encode(req.password()), role);
        userRepository.save(user);
        var userDetails = userDetailsService.loadUserByUsername(req.email());
        return new AuthResponse(jwtService.generateToken(userDetails));
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        var userDetails = userDetailsService.loadUserByUsername(req.email());
        return new AuthResponse(jwtService.generateToken(userDetails));
    }
}
