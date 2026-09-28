package zw.gov.dpdms.report.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts the raw per-hazard incident maps into one consistent table
 * shape: the shared columns every hazard has, plus a single "Indicators"
 * column summarizing whichever hazard-specific fields that record carries
 * (since each hazard has a different five fields, folding them into one
 * readable column keeps this report generic across hazards rather than
 * needing five separate report layouts).
 */
public final class ReportRowMapper {

    private ReportRowMapper() {}

    public static final String[] HEADER = {
            "ID", "Hazard", "Ward", "District", "Province", "Severity",
            "Status", "Occurred At", "Reported By", "Latitude", "Longitude", "Indicators"
    };

    private static final Set<String> SHARED_KEYS = Set.of(
            "id", "hazardType", "ward", "district", "province", "severity", "status",
            "occurredAt", "reportedBy", "latitude", "longitude",
            "reviewedBy", "reviewedAt", "reviewComment", "createdAt", "updatedAt"
    );

    public static List<String[]> toRows(List<Map<String, Object>> incidents) {
        List<String[]> rows = new ArrayList<>();
        for (Map<String, Object> incident : incidents) {
            rows.add(new String[]{
                    str(incident.get("id")),
                    str(incident.get("hazardType")),
                    str(incident.get("ward")),
                    str(incident.get("district")),
                    str(incident.get("province")),
                    str(incident.get("severity")),
                    str(incident.get("status")),
                    str(incident.get("occurredAt")),
                    str(incident.get("reportedBy")),
                    str(incident.get("latitude")),
                    str(incident.get("longitude")),
                    indicatorsSummary(incident)
            });
        }
        return rows;
    }

    private static String indicatorsSummary(Map<String, Object> incident) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (Map.Entry<String, Object> entry : incident.entrySet()) {
            if (!SHARED_KEYS.contains(entry.getKey()) && entry.getValue() != null) {
                parts.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return String.join("; ", parts);
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
