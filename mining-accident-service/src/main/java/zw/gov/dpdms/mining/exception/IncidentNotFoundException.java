package zw.gov.dpdms.mining.exception;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(Long id) {
        super("MiningAccident incident " + id + " was not found");
    }
}
