package zw.gov.dpdms.report.exception;

public class ForbiddenReportAccessException extends RuntimeException {
    public ForbiddenReportAccessException(String message) {
        super(message);
    }
}
