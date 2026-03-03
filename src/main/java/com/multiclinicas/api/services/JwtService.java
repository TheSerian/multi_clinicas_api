package com.multiclinicas.api.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${api.security.token.secret:mySecretKeyWithAtLeast32CharactersLongToEnsureHS256Algorithm}")
    private String secretKey;

    @Value("${api.security.token.expiration:86400000}")
    private long jwtExpiration;

    public String generateToken(Long userId, String role, Long clinicId) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        extraClaims.put("role", role);
        if (clinicId != null) {
            extraClaims.put("clinicId", clinicId);
        }

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userId.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(generateBase64Secret(secretKey));
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    private String generateBase64Secret(String secret) {
        if (secret.length() < 32) {
            StringBuilder sb = new StringBuilder(secret);
            while(sb.length() < 32){
                sb.append(secret);
            }
            return java.util.Base64.getEncoder().encodeToString(sb.toString().substring(0, 32).getBytes());
        }
        return java.util.Base64.getEncoder().encodeToString(secret.getBytes());
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Long extractClinicId(String token) {
        Object clinicIdObj = extractAllClaims(token).get("clinicId");
        if (clinicIdObj == null) {
            return null;
        }
        return Long.valueOf(clinicIdObj.toString());
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
