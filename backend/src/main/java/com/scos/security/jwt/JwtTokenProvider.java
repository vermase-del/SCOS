package com.scos.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${scos.jwt.secret}")
    private String jwtSecret;

    @Value("${scos.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UUID userId, String username, List<String> roles, UUID activeCommunityId) {
        return generateToken(userId, username, roles, null, activeCommunityId);
    }

    public String generateToken(UUID userId, String username, List<String> roles, List<String> permissions, UUID activeCommunityId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("roles", roles);
        if (permissions != null) {
            claims.put("permissions", permissions);
        }
        if (activeCommunityId != null) {
            claims.put("communityId", activeCommunityId.toString());
        }

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String username = claims.getSubject();

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String r : roles) {
                authorities.add(new SimpleGrantedAuthority(r));
                if (!r.startsWith("ROLE_")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) claims.get("permissions");
        if (permissions != null) {
            for (String p : permissions) {
                authorities.add(new SimpleGrantedAuthority(p));
            }
        }

        User principal = new User(username, "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString((String) claims.get("userId"));
    }
}
