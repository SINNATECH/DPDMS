package zw.gov.dpdms.dashboard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${dpdms.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public DpdmsPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new DpdmsPrincipal(
                claims.getSubject(),
                DpdmsPrincipal.Role.valueOf(claims.get("role", String.class)),
                claims.get("hazard", String.class),
                claims.get("ward", String.class)
        );
    }
}
