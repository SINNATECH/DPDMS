package zw.gov.dpdms.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A single user account. In this starter, accounts live in an in-memory
 * list (see InMemoryUserStore) rather than a database — replace that
 * store with a real users table/repository before submission, keeping
 * this class's shape (and the BCrypt-hashed password) the same.
 */
@Getter
@AllArgsConstructor
public class AuthUser {

    private final String username;
    private final String passwordHash; // BCrypt hash, never plaintext
    private final Role role;
    private final String hazard; // e.g. "FLOOD" — null for NATIONAL/ADMIN
    private final String ward;   // only meaningful for RECORDER — null otherwise
}
