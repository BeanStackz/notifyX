package com.beanstack.notifyx.identity.security;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service 
public class JwtService {
    
    private final SecretKey signingKey;
    private final long accessTokenExpiration;

    public JwtService( 
        @Value("${security.jwt.secret}")
        String secret, 
        @Value ("${security.jwt.access-token-expiration}")
        long accessTokenExpiration) {

            byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            try {
                java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
                keyBytes = sha.digest(keyBytes);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException("Error initializing JWT signing key", e);
            }


            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(UserDetails userDetails) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + accessTokenExpiration);
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(signingKey)
            .compact();    
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }

}
