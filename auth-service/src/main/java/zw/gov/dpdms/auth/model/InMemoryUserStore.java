package zw.gov.dpdms.auth.model;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Seeded demo accounts, hashed with BCrypt at startup. This is a stand-in
 * for a real users table — before submission, replace find() with a
 * repository-backed lookup and move these seed rows into your DB's seed
 * data script instead.
 *
 * Password for every seeded account is "password123" — change this
 * before showing the demo to anyone outside your group.
 */
@Component
public class InMemoryUserStore {

    private final List<AuthUser> users;

    public InMemoryUserStore(PasswordEncoder passwordEncoder) {
        String hash = passwordEncoder.encode("password123");

        this.users = List.of(
                // Ward recorders — one per hazard, each scoped to a single ward
                new AuthUser("flood.recorder", hash, Role.RECORDER, "FLOOD", "Ward 3"),
                new AuthUser("drought.recorder", hash, Role.RECORDER, "DROUGHT", "Ward 5"),
                new AuthUser("fire.recorder", hash, Role.RECORDER, "FIRE", "Ward 2"),
                new AuthUser("zoonotic.recorder", hash, Role.RECORDER, "ZOONOTIC_DISEASE", "Ward 4"),
                new AuthUser("mining.recorder", hash, Role.RECORDER, "MINING_ACCIDENT", "Ward 1"),

                // Provincial supervisors — one per hazard, no ward
                new AuthUser("flood.supervisor", hash, Role.SUPERVISOR, "FLOOD", null),
                new AuthUser("drought.supervisor", hash, Role.SUPERVISOR, "DROUGHT", null),
                new AuthUser("fire.supervisor", hash, Role.SUPERVISOR, "FIRE", null),
                new AuthUser("zoonotic.supervisor", hash, Role.SUPERVISOR, "ZOONOTIC_DISEASE", null),
                new AuthUser("mining.supervisor", hash, Role.SUPERVISOR, "MINING_ACCIDENT", null),

                // Cross-hazard read-only, and a full-access admin
                new AuthUser("national.user", hash, Role.NATIONAL, null, null),
                new AuthUser("admin", hash, Role.ADMIN, null, null)
        );
    }

    public Optional<AuthUser> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }
}
