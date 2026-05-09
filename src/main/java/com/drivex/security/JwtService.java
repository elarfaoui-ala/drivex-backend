package com.drivex.security;

import com.drivex.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties props;

    // ── Token generation ──────────────────────────────────────────────────────

    public String generateAccessToken(UserDetails user) {
        return generateAccessToken(Map.of(), user);
    }

    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails user) {
        return buildToken(extraClaims, user, props.getExpirationMs());
    }

    public String generateRefreshToken(UserDetails user) {
        return buildToken(Map.of(), user, props.getRefreshExpirationMs());
    }

    private String buildToken(Map<String, Object> extra, UserDetails user, long expirationMs) {
        var claims = new HashMap<>(extra);
        claims.put("roles", user.getAuthorities());

        return Jwts.builder()
            .claims(claims)
            .subject(user.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey(), Jwts.SIG.HS256)
            .compact();
    }

    // ── Token validation ──────────────────────────────────────────────────────

    public boolean isValid(String token, UserDetails user) {
        try {
            final String subject = extractSubject(token);
            return subject.equals(user.getUsername()) && !isExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // ── Claims extraction ─────────────────────────────────────────────────────

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey signingKey() {
        // If secret is not Base64, use it as raw bytes
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(props.getSecret());
        } catch (Exception e) {
            bytes = props.getSecret().getBytes();
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
