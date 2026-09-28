package zw.gov.dpdms.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import zw.gov.dpdms.auth.dto.LoginRequest;
import zw.gov.dpdms.auth.dto.LoginResponse;
import zw.gov.dpdms.auth.exception.InvalidCredentialsException;
import zw.gov.dpdms.auth.model.AuthUser;
import zw.gov.dpdms.auth.model.InMemoryUserStore;
import zw.gov.dpdms.auth.security.JwtIssuer;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final InMemoryUserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtIssuer jwtIssuer;

    public LoginResponse login(LoginRequest request) {
        AuthUser user = userStore.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        JwtIssuer.IssuedToken issued = jwtIssuer.issue(user);

        return LoginResponse.builder()
                .token(issued.token())
                .tokenType("Bearer")
                .expiresAt(issued.expiresAt())
                .username(user.getUsername())
                .role(user.getRole())
                .hazard(user.getHazard())
                .ward(user.getWard())
                .build();
    }
}
