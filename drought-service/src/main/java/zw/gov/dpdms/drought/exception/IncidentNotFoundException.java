package zw.gov.dpdms.drought.exception;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(Long id) {
        super("Drought incident " + id + " was not found");
    }
}
