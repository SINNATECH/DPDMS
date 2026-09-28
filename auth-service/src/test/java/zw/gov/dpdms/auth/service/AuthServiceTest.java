package zw.gov.dpdms.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import zw.gov.dpdms.auth.dto.LoginRequest;
import zw.gov.dpdms.auth.dto.LoginResponse;
import zw.gov.dpdms.auth.exception.InvalidCredentialsException;
import zw.gov.dpdms.auth.model.InMemoryUserStore;
import zw.gov.dpdms.auth.security.JwtIssuer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private static final String TEST_SECRET = "test-secret-at-least-32-characters-long-ok";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final InMemoryUserStore userStore = new InMemoryUserStore(passwordEncoder);
    private final JwtIssuer jwtIssuer = new JwtIssuer(TEST_SECRET);
    private final AuthService authService = new AuthService(userStore, passwordEncoder, jwtIssuer);

    private LoginRequest request(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    @Test
    void correctCredentialsReturnATokenWithMatchingClaims() {
        LoginResponse response = authService.login(request("flood.recorder", "password123"));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo("flood.recorder");
        assertThat(response.getHazard()).isEqualTo("FLOOD");
        assertThat(response.getWard()).isEqualTo("Ward 3");
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThatThrownBy(() -> authService.login(request("flood.recorder", "wrongpassword")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void unknownUsernameIsRejected() {
        assertThatThrownBy(() -> authService.login(request("nobody", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void nationalUserHasNoHazardOrWard() {
        LoginResponse response = authService.login(request("national.user", "password123"));

        assertThat(response.getHazard()).isNull();
        assertThat(response.getWard()).isNull();
    }
}
