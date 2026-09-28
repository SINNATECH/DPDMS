package zw.gov.dpdms.report.dto;

import java.time.LocalDate;

/**
 * Every filter the brief asks reports to support: hazard type, ward,
 * district, date range, severity, and approval status. Any field left
 * null means "don't filter on this dimension."
 */
public record ReportFilter(
        String hazard,
        String ward,
        String district,
        LocalDate startDate,
        LocalDate endDate,
        String severity,
        String status
) {}
