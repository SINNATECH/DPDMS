package zw.gov.dpdms.auth.model;

/**
 * Mirrors DpdmsPrincipal.Role in every hazard service exactly — the string
 * value written into the JWT's "role" claim must match one of these names
 * for the hazard services to accept it.
 */
public enum Role {
    RECORDER,
    SUPERVISOR,
    NATIONAL,
    ADMIN
}
