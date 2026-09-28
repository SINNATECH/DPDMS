package zw.gov.dpdms.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zw.gov.dpdms.auth.model.AuthUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues JWTs using the exact same HMAC secret every hazard service's
 * JwtService uses to verify them (dpdms.jwt.secret / JWT_SECRET env var).
 * Keep this secret identical across every service in the system — that
 * shared trust is what lets one login work everywhere.
 */
@Component
public class JwtIssuer {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(8);

    private final SecretKey signingKey;

    public JwtIssuer(@Value("${dpdms.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issue(AuthUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(TOKEN_LIFETIME);

        var builder = Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (user.getHazard() != null) {
            builder.claim("hazard", user.getHazard());
        }
        if (user.getWard() != null) {
            builder.claim("ward", user.getWard());
        }

        String token = builder.signWith(signingKey).compact();
        return new IssuedToken(token, expiry);
    }

    public record IssuedToken(String token, Instant expiresAt) {}
}
