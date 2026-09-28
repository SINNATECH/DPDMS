package zw.gov.dpdms.zoonotic.alert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Tells alert-service that an incident has just been approved so it can
 * check the alert criteria and dispatch email/WhatsApp alerts.
 *
 * Fire-and-forget by design:
 *  - @Async, so the supervisor's approve request never waits on it.
 *  - Every failure is caught and logged, so if alert-service is down or
 *    slow the approval still succeeds (the brief requires the system to
 *    degrade gracefully when the alerting service is unavailable).
 */
@Component
@Slf4j
public class AlertNotifier {

    private static final String EVALUATE_URL = "http://alert-service/internal/alerts/evaluate";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String internalKey;

    public AlertNotifier(RestTemplate restTemplate,
                         ObjectMapper objectMapper,
                         @Value("${ALERT_INTERNAL_KEY:change-me-internal-key}") String internalKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.internalKey = internalKey;
    }

    @Async
    public void notifyApproved(String hazardType, Object incident) {
        try {
            Map<String, Object> incidentMap =
                    objectMapper.convertValue(incident, new TypeReference<Map<String, Object>>() {});

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Key", internalKey);

            Map<String, Object> payload = Map.of("hazardType", hazardType, "incident", incidentMap);
            restTemplate.postForEntity(EVALUATE_URL, new HttpEntity<>(payload, headers), Void.class);
        } catch (Exception ex) {
            log.warn("Could not notify alert-service about approved {} incident: {}", hazardType, ex.getMessage());
        }
    }
}
