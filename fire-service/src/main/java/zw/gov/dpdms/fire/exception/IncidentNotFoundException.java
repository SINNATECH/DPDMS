package zw.gov.dpdms.fire.exception;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(Long id) {
        super("Fire incident " + id + " was not found");
    }
}
