package com.example.ticketing.unit.infra;

import com.example.ticketing.auth.infrastructure.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.*;

@Tag("unit")
class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-key-at-least-32-chars-long!!";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    private User userDetails(String email) {
        return (User) User.withUsername(email)
                .password("pass")
                .authorities("ROLE_BUYER")
                .build();
    }

    @Test
    void generateAndExtract_roundTrip() {
        var details = userDetails("alice@test.com");
        String token = jwtService.generateToken(details);
        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@test.com");
    }

    @Test
    void isValid_validToken_returnsTrue() {
        var details = userDetails("alice@test.com");
        String token = jwtService.generateToken(details);
        assertThat(jwtService.isValid(token, details)).isTrue();
    }

    @Test
    void isValid_wrongUser_returnsFalse() {
        var alice = userDetails("alice@test.com");
        var bob = userDetails("bob@test.com");
        String token = jwtService.generateToken(alice);
        assertThat(jwtService.isValid(token, bob)).isFalse();
    }

    @Test
    void expiredToken_throwsJwtException() {
        JwtService shortLived = new JwtService(SECRET, 1L);
        var details = userDetails("alice@test.com");
        String token = shortLived.generateToken(details);
        // expiration is 1ms — by the time we call extractEmail, it's expired
        assertThatThrownBy(() -> shortLived.extractEmail(token))
                .isInstanceOf(JwtException.class);
    }
}
