package zw.gov.dpdms.webclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Every HTTP call from web-client to the backend goes through this one
 * class, and every call goes to the gateway (never directly to a hazard
 * service) — that's what proves the gateway routing/CORS setup actually
 * works, rather than the front end quietly bypassing it.
 */
@Service
public class GatewayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String gatewayBaseUrl;

    public GatewayClient(RestTemplate restTemplate,
                          ObjectMapper objectMapper,
                          @Value("${dpdms.gateway.base-url}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    /** Calls POST /api/auth/login and returns the raw response as a Map. */
    public Map<String, Object> login(String username, String password) {
        HttpHeaders headers = jsonHeaders(null);
        HttpEntity<Map<String, String>> entity =
                new HttpEntity<>(Map.of("username", username, "password", password), headers);

        return restTemplate.postForObject(
                gatewayBaseUrl + "/api/auth/login", entity, Map.class);
    }

    /** Lists incidents for the given hazard path (e.g. "floods"). */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list(String hazardPath, String token, boolean onlyApproved) {
        HttpHeaders headers = jsonHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = gatewayBaseUrl + "/api/" + hazardPath + "?onlyApproved=" + onlyApproved;
        Map<String, Object>[] result = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map[].class).getBody();

        return result == null ? List.of() : List.of(result);
    }

    /** Creates a new incident. body should contain both shared and hazard-specific fields. */
    public Map<String, Object> create(String hazardPath, String token, Map<String, Object> body) {
        HttpHeaders headers = jsonHeaders(token);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(
                gatewayBaseUrl + "/api/" + hazardPath, entity, Map.class);
    }

    public Map<String, Object> approve(String hazardPath, String token, Long id) {
        return reviewAction(hazardPath, token, id, "approve", null);
    }

    public Map<String, Object> reject(String hazardPath, String token, Long id, String comment) {
        return reviewAction(hazardPath, token, id, "reject", comment);
    }

    public Map<String, Object> requestCorrections(String hazardPath, String token, Long id, String comment) {
        return reviewAction(hazardPath, token, id, "request-corrections", comment);
    }

    private Map<String, Object> reviewAction(String hazardPath, String token, Long id,
                                              String action, String comment) {
        HttpHeaders headers = jsonHeaders(token);
        Map<String, Object> body = comment == null ? Map.of() : Map.of("comment", comment);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(
                gatewayBaseUrl + "/api/" + hazardPath + "/" + id + "/" + action, entity, Map.class);
    }

    /**
     * Pulls a readable message out of a backend error response, since the
     * hazard services return structured JSON like {"message": "..."} for
     * every 4xx/5xx — falls back to the raw exception message if parsing
     * fails for any reason.
     */
    public String extractErrorMessage(HttpStatusCodeException ex) {
        try {
            Map<?, ?> body = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
            Object message = body.get("message");
            return message != null ? message.toString() : ex.getMessage();
        } catch (Exception parseFailure) {
            return ex.getMessage();
        }
    }

    private HttpHeaders jsonHeaders(String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null) {
            headers.setBearerAuth(bearerToken);
        }
        return headers;
    }
}
