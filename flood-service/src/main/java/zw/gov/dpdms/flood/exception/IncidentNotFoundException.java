package zw.gov.dpdms.flood.exception;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(Long id) {
        super("Flood incident " + id + " was not found");
    }
}
