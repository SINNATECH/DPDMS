package zw.gov.dpdms.flood.security;

/**
 * The claims every DPDMS service needs to enforce hazard-level scoping.
 * Populated from the validated JWT by JwtAuthenticationFilter — nothing
 * in a request body or query param is ever trusted for authorization.
 */
public record DpdmsPrincipal(
        String username,
        Role role,
        String hazard,   // e.g. "FLOOD" — null for NATIONAL/ADMIN
        String ward      // only meaningful for RECORDER
) {
    public enum Role { RECORDER, SUPERVISOR, NATIONAL, ADMIN }

    public boolean isRecorder() {
        return role == Role.RECORDER;
    }

    public boolean isSupervisor() {
        return role == Role.SUPERVISOR;
    }

    public boolean isNational() {
        return role == Role.NATIONAL;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isScopedToHazard(String requiredHazard) {
    	return isAdmin() || isNational() || requiredHazard.equalsIgnoreCase(hazard);
    }
}
