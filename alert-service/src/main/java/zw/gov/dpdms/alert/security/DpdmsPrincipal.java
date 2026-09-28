package zw.gov.dpdms.alert.security;

public record DpdmsPrincipal(
        String username,
        Role role,
        String hazard,
        String ward
) {
    public enum Role { RECORDER, SUPERVISOR, NATIONAL, ADMIN }

    public boolean isRecorder() { return role == Role.RECORDER; }
    public boolean isSupervisor() { return role == Role.SUPERVISOR; }
    public boolean isNational() { return role == Role.NATIONAL; }
    public boolean isAdmin() { return role == Role.ADMIN; }
}
