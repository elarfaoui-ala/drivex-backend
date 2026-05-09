package com.drivex;

import com.drivex.config.JwtProperties;
import com.drivex.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    JwtService jwtService;

    @BeforeEach
    void setUp() {
        var props = new JwtProperties();
        props.setSecret("drivex-test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        props.setExpirationMs(3_600_000L);        // 1 hour
        props.setRefreshExpirationMs(86_400_000L); // 24 hours
        jwtService = new JwtService(props);
    }

    private UserDetails user(String email) {
        return User.withUsername(email).password("x").authorities(List.of()).build();
    }

    @Test
    void generateAndValidate_accessToken_shouldBeValid() {
        UserDetails user  = user("alex@drivex.com");
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token, user)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("alex@drivex.com");
    }

    @Test
    void token_shouldNotBeValidForDifferentUser() {
        UserDetails alex = user("alex@drivex.com");
        UserDetails sara = user("sara@drivex.com");
        String token = jwtService.generateAccessToken(alex);

        assertThat(jwtService.isValid(token, sara)).isFalse();
    }

    @Test
    void generateRefreshToken_shouldHaveDifferentSubjectButSameEmail() {
        UserDetails user    = user("alex@drivex.com");
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isExpired(refreshToken)).isFalse();
        assertThat(jwtService.extractSubject(refreshToken)).isEqualTo("alex@drivex.com");
    }

    @Test
    void expiredToken_shouldNotBeValid() {
        var props = new JwtProperties();
        props.setSecret("drivex-test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        props.setExpirationMs(-1000L); // already expired
        props.setRefreshExpirationMs(86_400_000L);
        var expiredService = new JwtService(props);

        UserDetails user  = user("alex@drivex.com");
        String token = expiredService.generateAccessToken(user);

        assertThat(expiredService.isExpired(token)).isTrue();
        assertThat(expiredService.isValid(token, user)).isFalse();
    }

    @Test
    void malformedToken_isValid_shouldReturnFalse() {
        UserDetails user = user("alex@drivex.com");
        assertThat(jwtService.isValid("this.is.not.a.jwt", user)).isFalse();
    }
}
