package com.orcafin.security;

import com.orcafin.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Set<String> KNOWN_DEFAULT_SECRETS = Set.of(
            "change-this-secret-to-a-long-random-base64-string-in-production-please",
            "troque-este-segredo-por-uma-string-base64-longa-e-aleatoria-em-producao",
            "troque-por-uma-string-base64-longa-e-aleatoria"
    );

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @PostConstruct
    void validateSecret() {
        if (KNOWN_DEFAULT_SECRETS.contains(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET está usando o valor padrão de exemplo. Defina uma variável de ambiente " +
                            "JWT_SECRET própria e forte (mínimo 32 bytes aleatórios) antes de iniciar a aplicação.");
        }
        if (secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET é curto demais (mínimo 32 bytes / 256 bits para HMAC-SHA256). Gere um valor " +
                            "mais forte, ex.: openssl rand -base64 32");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    private static final String CLAIM_PWD_CHANGED_AT = "pwdChangedAt";

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(CLAIM_PWD_CHANGED_AT, user.getPasswordChangedAt().toEpochMilli())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, User user) {
        String username = extractUsername(token);
        long tokenPwdChangedAt = extractClaim(token, c -> c.get(CLAIM_PWD_CHANGED_AT, Long.class));
        return username.equals(user.getUsername())
                && tokenPwdChangedAt == user.getPasswordChangedAt().toEpochMilli()
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }
}
