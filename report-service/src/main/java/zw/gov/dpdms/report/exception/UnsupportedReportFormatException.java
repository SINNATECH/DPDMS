package zw.gov.dpdms.report.exception;

public class UnsupportedReportFormatException extends RuntimeException {
    public UnsupportedReportFormatException(String format) {
        super("Unsupported report format: " + format + " — use csv, xlsx, docx, or pdf");
    }
}
