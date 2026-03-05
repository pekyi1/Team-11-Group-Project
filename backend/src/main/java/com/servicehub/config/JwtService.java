package com.servicehub.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration-ms}") private long expiration;
    @Value("${jwt.refresh-expiration-ms}") private long refreshExpiration;

    private Key getSigningKey() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generateToken(String email, String role) {
        return Jwts.builder().subject(email).claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey()).compact();
    }

    public String generateRefreshToken(String email, String role) {
        return Jwts.builder().subject(email).claim("role", role).claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey()).compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getPayload().getSubject();
    }

    public String extractRole(String token) {
        return Jwts.parser().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getPayload().get("role", String.class);
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = Jwts.parser().setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token).getPayload().get("type", String.class);
            return "refresh".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isTokenValid(String token) {
        try { Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token); return true; }
        catch (JwtException e) { return false; }
    }
}
