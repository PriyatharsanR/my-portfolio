package com.backend.portfolio.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (expiration == null || expiration <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }


    public String generateToken(String username, long version) {
        if (username == null || username.isBlank() || version < 0) {
            throw new IllegalArgumentException("JWT requires a username and nonnegative credential version");
        }
        return Jwts.builder()
                .setSubject(username)
                .claim("ver", version)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+ expiration))
                .signWith(secretKey)
                .compact();
    }


    public String extractUsername(String token) {
        return parseToken(token).username();
    }


    public TokenIdentity parseToken(String token) {
        Claims claims = getClaims(token);
        String username = claims.getSubject();
        Object rawVersion = claims.get("ver");
        if (username == null || username.isBlank() || claims.getExpiration() == null
                || !claims.getExpiration().after(new Date())
                || !(rawVersion instanceof Integer || rawVersion instanceof Long)
                || ((Number) rawVersion).longValue() < 0) {
            throw new JwtException("Invalid token claims");
        }
        return new TokenIdentity(username, ((Number) rawVersion).longValue());
    }


    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public record TokenIdentity(String username, long version) {
    }
}
