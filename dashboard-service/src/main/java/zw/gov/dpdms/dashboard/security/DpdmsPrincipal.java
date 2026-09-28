package zw.gov.dpdms.dashboard.security;

/**
 * Mirrors DpdmsPrincipal in every hazard service. dashboard-service only
 * ever needs role here (NATIONAL/ADMIN are the two roles allowed to call
 * it), but the full shape is kept for consistency with the rest of the
 * system and in case ward/hazard-scoped dashboards are added later.
 */
public record DpdmsPrincipal(
        String username,
        Role role,
        String hazard,
        String ward
) {
    public enum Role { RECORDER, SUPERVISOR, NATIONAL, ADMIN }

    public boolean isNational() {
        return role == Role.NATIONAL;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
