package com.suburbscore.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET      = "suburbscore-super-secret-jwt-key-must-be-256-bits-long!!";
    private static final long   EXPIRY_MS   = 86400000L;
    private static final String EMAIL       = "john.smith@example.com";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", EXPIRY_MS);
    }

    @Test
    @DisplayName("generateToken — returns non-null JWT string")
    void generateToken_returnsToken() {
        String token = jwtUtil.generateToken(EMAIL);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractEmail — returns the email used to generate the token")
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken(EMAIL);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("validateToken — valid token returns true")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(EMAIL);

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken — tampered token returns false")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(EMAIL);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken — empty string returns false")
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("validateToken — expired token returns false")
    void validateToken_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", -1000L);
        String expiredToken = jwtUtil.generateToken(EMAIL);

        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", EXPIRY_MS);

        assertThat(jwtUtil.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("getExpirationMs — returns configured value")
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtUtil.getExpirationMs()).isEqualTo(EXPIRY_MS);
    }
}
