package com.vidyapeet.security;

import com.vidyapeet.common.Role;
import com.vidyapeet.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues and validates stateless JWTs. The token embeds the user id, role and
 * institute id; the institute id drives tenant scoping for the whole request.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${vidyapeet.jwt.secret}") String secret,
            @Value("${vidyapeet.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .claim("instituteId", user.getInstituteId())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public UserPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get("role", String.class));
        Number instituteIdClaim = claims.get("instituteId", Number.class);
        Long instituteId = instituteIdClaim == null ? null : instituteIdClaim.longValue();
        String email = claims.get("email", String.class);
        return new UserPrincipal(userId, instituteId, email, role);
    }
}
