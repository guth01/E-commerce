package com.example.miniproject.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority; // Added import
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List; // Added import
import java.util.stream.Collectors; // Added import

@Component
public class JwtUtils {
    private final String jwtSecret = "ReplaceWithAStrongSecretKeyOfAtLeast32Characters!!";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        // Extract just the String names of the roles from the authority objects
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(authentication.getName())
                .claim("roles", roles) // Pass the plain list of Strings here
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + 86400000)) // 24 hours
                .signWith(getSigningKey())
                .compact();
    }
    // Extracts the username from the token payload
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Checks if the token is valid, not expired, and properly signed
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
            return true;
        } catch (Exception e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
        }
        return false;
    }
}