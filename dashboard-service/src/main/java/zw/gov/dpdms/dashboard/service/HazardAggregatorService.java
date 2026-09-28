package zw.gov.dpdms.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import zw.gov.dpdms.dashboard.util.HazardRegistry;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Every method here calls all five hazard services and merges the
 * results. A single hazard service being down (or slow, or returning an
 * error) never fails the whole dashboard — it's simply excluded from
 * that response, satisfying the brief's "must degrade gracefully"
 * non-functional requirement rather than the dashboard being an
 * all-or-nothing single point of failure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HazardAggregatorService {

    private final RestTemplate restTemplate;

    /** Fetches approved incidents from every hazard service, tagging each with its hazard type. */
    public List<Map<String, Object>> fetchAllApproved(String bearerToken) {
        List<Map<String, Object>> combined = new ArrayList<>();

        for (String hazard : HazardRegistry.ALL_HAZARDS) {
            HazardRegistry.HazardEndpoint endpoint = HazardRegistry.ENDPOINTS.get(hazard);
            try {
                List<Map<String, Object>> incidents = fetchFromOneHazard(endpoint, bearerToken);
                for (Map<String, Object> incident : incidents) {
                    incident.put("hazardType", hazard);
                }
                combined.addAll(incidents);
            } catch (Exception ex) {
                // Degrade gracefully: log and continue with the other four hazards
                // rather than letting one down/slow service break the whole dashboard.
                log.warn("Dashboard could not reach {} ({}): {}", endpoint.serviceId(),
                        hazard, ex.getMessage());
            }
        }

        return combined;
    }

    public Map<String, Object> summary(String bearerToken) {
        List<Map<String, Object>> all = fetchAllApproved(bearerToken);

        Map<String, Long> byHazard = all.stream()
                .collect(Collectors.groupingBy(i -> String.valueOf(i.get("hazardType")), Collectors.counting()));

        Map<String, Long> bySeverity = all.stream()
                .collect(Collectors.groupingBy(i -> String.valueOf(i.get("severity")), Collectors.counting()));

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(i -> String.valueOf(i.get("status")), Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalApprovedIncidents", all.size());
        result.put("byHazard", byHazard);
        result.put("bySeverity", bySeverity);
        result.put("byStatus", byStatus);
        result.put("generatedAt", LocalDateTime.now().toString());
        return result;
    }

    public List<Map<String, Object>> recent(String bearerToken, int limit) {
        return fetchAllApproved(bearerToken).stream()
                .sorted(Comparator.comparing(
                        (Map<String, Object> i) -> String.valueOf(i.get("occurredAt"))).reversed())
                .limit(limit)
                .toList();
    }

    /** Thin projection for map-plotting: just id, hazard, ward, severity, coordinates. */
    public List<Map<String, Object>> mapPoints(String bearerToken) {
        return fetchAllApproved(bearerToken).stream()
                .map(i -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("id", i.get("id"));
                    point.put("hazardType", i.get("hazardType"));
                    point.put("ward", i.get("ward"));
                    point.put("severity", i.get("severity"));
                    point.put("latitude", i.get("latitude"));
                    point.put("longitude", i.get("longitude"));
                    return point;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchFromOneHazard(HazardRegistry.HazardEndpoint endpoint, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = "http://" + endpoint.serviceId() + "/api/" + endpoint.apiPath() + "?onlyApproved=true";
        Map<String, Object>[] response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map[].class).getBody();

        return response == null ? List.of() : new ArrayList<>(List.of(response));
    }
}
