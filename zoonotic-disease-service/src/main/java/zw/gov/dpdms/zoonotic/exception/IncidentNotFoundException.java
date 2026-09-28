package zw.gov.dpdms.zoonotic.exception;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(Long id) {
        super("ZoonoticDisease incident " + id + " was not found");
    }
}
