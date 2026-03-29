package com.docusign.docusign.config;

import com.docusign.docusign.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;

@Service
public class JwtService {

    // Secret key to sign tokens - should be in application.properties later
   // private static final String SECRET_KEY = "your-super-secret-key-that-is-long-enough";
    //private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long EXPIRATION_TIME;

    // Generate token from user
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())        // who this token belongs to
                .setIssuedAt(new Date())             // when it was created
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // expiry
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // sign it
                .compact();
    }

    // Extract email from token
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    // Is token valid?
    public boolean isTokenValid(String token, User user) {
        String email = extractEmail(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }

    // Is token expired?
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    // Extract all claims from token
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Convert secret key string to signing key
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);  //⚠️ One thing — SECRET_KEY needs to be a Base64 encoded string that's long enough. For now use this for testing SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
        return Keys.hmacShaKeyFor(keyBytes);
    }
}