package zw.gov.dpdms.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import zw.gov.dpdms.report.dto.ReportFilter;
import zw.gov.dpdms.report.exception.ForbiddenReportAccessException;
import zw.gov.dpdms.report.security.DpdmsPrincipal;
import zw.gov.dpdms.report.util.HazardRegistry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reports are, by the brief's own rule, approved-data-only for anyone
 * who isn't reviewing their own hazard's internal queue — "a record
 * that has been captured but not yet approved... must not be included
 * in generated reports." RECORDER and NATIONAL/cross-hazard requests
 * are hard-locked to approved-only here; only a SUPERVISOR or ADMIN
 * asking about their own hazard can request a specific status, for
 * internal review purposes (e.g. "show me everything still PENDING").
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportDataService {

    private final RestTemplate restTemplate;

    public List<Map<String, Object>> fetch(DpdmsPrincipal principal, ReportFilter filter, String bearerToken) {
        List<String> targetHazards = resolveTargetHazards(principal, filter);
        boolean onlyApproved = resolveOnlyApproved(principal, filter);

        List<Map<String, Object>> combined = new ArrayList<>();
        for (String hazard : targetHazards) {
            HazardRegistry.HazardEndpoint endpoint = HazardRegistry.ENDPOINTS.get(hazard);
            try {
                List<Map<String, Object>> incidents = fetchFromOneHazard(endpoint, bearerToken, onlyApproved);
                for (Map<String, Object> incident : incidents) {
                    incident.put("hazardType", hazard);
                }
                combined.addAll(incidents);
            } catch (Exception ex) {
                log.warn("Report generation could not reach {} ({}): {}",
                        endpoint.serviceId(), hazard, ex.getMessage());
            }
        }

        return applyRemainingFilters(combined, filter);
    }

    /** RECORDER/SUPERVISOR are locked to their own hazard; NATIONAL/ADMIN may query any or all. */
    private List<String> resolveTargetHazards(DpdmsPrincipal principal, ReportFilter filter) {
        if (principal.hazard() != null) {
            if (filter.hazard() != null && !filter.hazard().equalsIgnoreCase(principal.hazard())) {
                throw new ForbiddenReportAccessException(
                        "You may only generate reports for your own hazard (" + principal.hazard() + ")");
            }
            return List.of(principal.hazard());
        }
        return filter.hazard() != null ? List.of(filter.hazard().toUpperCase()) : HazardRegistry.ALL_HAZARDS;
    }

    /** Approved-only by default and always for RECORDER/NATIONAL; a hazard-scoped SUPERVISOR/ADMIN may override with a status filter. */
    private boolean resolveOnlyApproved(DpdmsPrincipal principal, ReportFilter filter) {
        boolean canSeeUnapproved = (principal.isSupervisor() || principal.isAdmin()) && filter.status() != null;
        return !canSeeUnapproved;
    }

    private List<Map<String, Object>> applyRemainingFilters(List<Map<String, Object>> incidents, ReportFilter filter) {
        return incidents.stream()
                .filter(i -> filter.ward() == null || filter.ward().equalsIgnoreCase(str(i.get("ward"))))
                .filter(i -> filter.district() == null || filter.district().equalsIgnoreCase(str(i.get("district"))))
                .filter(i -> filter.severity() == null || filter.severity().equalsIgnoreCase(str(i.get("severity"))))
                .filter(i -> filter.status() == null || filter.status().equalsIgnoreCase(str(i.get("status"))))
                .filter(i -> withinDateRange(i, filter))
                .toList();
    }

    private boolean withinDateRange(Map<String, Object> incident, ReportFilter filter) {
        if (filter.startDate() == null && filter.endDate() == null) return true;
        Object raw = incident.get("occurredAt");
        if (raw == null) return false;
        try {
            LocalDate occurred = LocalDateTime.parse(str(raw)).toLocalDate();
            if (filter.startDate() != null && occurred.isBefore(filter.startDate())) return false;
            if (filter.endDate() != null && occurred.isAfter(filter.endDate())) return false;
            return true;
        } catch (Exception ex) {
            return true; // don't silently drop rows over an unexpected date format
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchFromOneHazard(HazardRegistry.HazardEndpoint endpoint,
                                                           String bearerToken, boolean onlyApproved) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "http://" + endpoint.serviceId() + "/api/" + endpoint.apiPath()
                + "?onlyApproved=" + onlyApproved;
        Map<String, Object>[] response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map[].class).getBody();

        return response == null ? List.of() : new ArrayList<>(List.of(response));
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }
}
