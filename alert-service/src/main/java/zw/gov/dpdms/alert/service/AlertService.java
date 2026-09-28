package zw.gov.dpdms.alert.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import zw.gov.dpdms.alert.dto.AlertEvaluationResult;
import zw.gov.dpdms.alert.dto.AlertLogResponse;
import zw.gov.dpdms.alert.dto.IncidentAlertRequest;
import zw.gov.dpdms.alert.exception.ForbiddenAlertAccessException;
import zw.gov.dpdms.alert.model.AlertChannel;
import zw.gov.dpdms.alert.model.AlertLog;
import zw.gov.dpdms.alert.repository.AlertLogRepository;
import zw.gov.dpdms.alert.rules.AlertRuleEngine;
import zw.gov.dpdms.alert.security.DpdmsPrincipal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRuleEngine ruleEngine;
    private final AlertDispatcher dispatcher;
    private final AlertLogRepository repository;
    private final List<String> emailRecipients;
    private final List<String> whatsappRecipients;

    public AlertService(
            AlertRuleEngine ruleEngine,
            AlertDispatcher dispatcher,
            AlertLogRepository repository,
            @Value("${dpdms.alert.email.recipients:}") String emailRecipientsCsv,
            @Value("${dpdms.alert.whatsapp.recipients:}") String whatsappRecipientsCsv) {
        this.ruleEngine = ruleEngine;
        this.dispatcher = dispatcher;
        this.repository = repository;
        this.emailRecipients = parseCsv(emailRecipientsCsv);
        this.whatsappRecipients = parseCsv(whatsappRecipientsCsv);
    }

    /**
     * Checks the incident against the alert criteria and, if it qualifies,
     * queues one delivery per recipient per channel. Rule evaluation is
     * instant; the actual sending happens on a background thread pool, so
     * this method returns immediately.
     */
    public AlertEvaluationResult evaluate(IncidentAlertRequest request) {
        String hazard = request.getHazardType().toUpperCase();
        Map<String, Object> incident = request.getIncident();

        Optional<String> reason = ruleEngine.evaluate(hazard, incident);
        if (reason.isEmpty()) {
            return new AlertEvaluationResult(false, "No alert criteria met", 0);
        }

        Long incidentId = toLong(incident.get("id"));
        String ward = text(incident.get("ward"), null);
        String subject = buildSubject(hazard, incident);
        String body = buildBody(hazard, incident, reason.get());

        int queued = 0;
        for (String recipient : orPlaceholder(emailRecipients)) {
            dispatcher.dispatch(AlertChannel.EMAIL, recipient, hazard, incidentId, ward, subject, body);
            queued++;
        }
        for (String recipient : orPlaceholder(whatsappRecipients)) {
            dispatcher.dispatch(AlertChannel.WHATSAPP, recipient, hazard, incidentId, ward, subject, body);
            queued++;
        }

        return new AlertEvaluationResult(true, reason.get(), queued);
    }

    /**
     * ADMIN and NATIONAL can read the whole log (optionally filtered by hazard).
     * A SUPERVISOR only sees their own hazard's alerts. Ward recorders have no
     * access, consistent with the rest of the system's hazard-level scoping.
     */
    public List<AlertLogResponse> logs(DpdmsPrincipal principal, String hazardFilter) {
        boolean hasFilter = hazardFilter != null && !hazardFilter.isBlank();
        List<AlertLog> rows;

        if (principal.isAdmin() || principal.isNational()) {
            rows = hasFilter
                    ? repository.findTop200ByHazardTypeOrderByCreatedAtDesc(hazardFilter.toUpperCase())
                    : repository.findTop200ByOrderByCreatedAtDesc();
        } else if (principal.isSupervisor()) {
            if (hasFilter && !hazardFilter.equalsIgnoreCase(principal.hazard())) {
                throw new ForbiddenAlertAccessException(
                        "You may only view alerts for your own hazard (" + principal.hazard() + ")");
            }
            rows = repository.findTop200ByHazardTypeOrderByCreatedAtDesc(principal.hazard());
        } else {
            throw new ForbiddenAlertAccessException(
                    "Only supervisors, national, and admin accounts may view the alert log");
        }

        return rows.stream().map(AlertLogResponse::from).toList();
    }

    private String buildSubject(String hazard, Map<String, Object> incident) {
        return "[DPDMS ALERT] " + displayName(hazard) + " - " + text(incident.get("severity"), "UNRATED")
                + " in " + text(incident.get("district"), "unknown district")
                + " (" + text(incident.get("ward"), "unknown ward") + ")";
    }

    private String buildBody(String hazard, Map<String, Object> incident, String reason) {
        String lat = text(incident.get("latitude"), null);
        String lon = text(incident.get("longitude"), null);

        StringBuilder sb = new StringBuilder();
        sb.append("DPDMS ALERT: ").append(reason).append("\n\n");
        sb.append("Hazard: ").append(displayName(hazard)).append("\n");
        sb.append("Incident ID: ").append(text(incident.get("id"), "n/a")).append("\n");
        sb.append("Severity: ").append(text(incident.get("severity"), "n/a")).append("\n");
        sb.append("Ward: ").append(text(incident.get("ward"), "n/a")).append("\n");
        sb.append("District: ").append(text(incident.get("district"), "n/a")).append("\n");
        sb.append("Province: ").append(text(incident.get("province"), "n/a")).append("\n");
        sb.append("Occurred at: ").append(text(incident.get("occurredAt"), "n/a")).append("\n");
        if (lat != null && lon != null) {
            sb.append("Location: ").append(lat).append(", ").append(lon).append("\n");
            sb.append("Map: https://www.google.com/maps?q=").append(lat).append(",").append(lon).append("\n");
        }
        return sb.toString();
    }

    private static String displayName(String hazard) {
        String[] words = hazard.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    /** With no recipients configured we still log the attempt, using a null recipient as a marker. */
    private static List<String> orPlaceholder(List<String> recipients) {
        return recipients.isEmpty() ? Collections.<String>singletonList(null) : recipients;
    }

    private static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static Long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    private static String text(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }
}
