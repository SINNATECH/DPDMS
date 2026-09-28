package zw.gov.dpdms.fire.exception;

/** Thrown whenever a principal's hazard/ward scope doesn't cover the requested record. */
public class ForbiddenHazardAccessException extends RuntimeException {
    public ForbiddenHazardAccessException(String message) {
        super(message);
    }
}
