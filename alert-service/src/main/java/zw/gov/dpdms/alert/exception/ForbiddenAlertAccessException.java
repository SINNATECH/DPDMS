package zw.gov.dpdms.alert.exception;

public class ForbiddenAlertAccessException extends RuntimeException {
    public ForbiddenAlertAccessException(String message) {
        super(message);
    }
}
