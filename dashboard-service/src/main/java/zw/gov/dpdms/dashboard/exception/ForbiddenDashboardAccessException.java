package zw.gov.dpdms.dashboard.exception;

public class ForbiddenDashboardAccessException extends RuntimeException {
    public ForbiddenDashboardAccessException(String message) {
        super(message);
    }
}
