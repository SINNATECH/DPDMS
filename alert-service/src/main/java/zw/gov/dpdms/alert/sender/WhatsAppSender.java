package zw.gov.dpdms.alert.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends alerts through the WhatsApp Business Cloud API (Meta Graph API).
 * The phone number id and access token come from environment variables
 * (WHATSAPP_PHONE_NUMBER_ID, WHATSAPP_ACCESS_TOKEN), never from code.
 *
 * NOTE: WhatsApp only allows free-form text messages to a recipient who has
 * messaged the business number in the last 24 hours. Messages started by the
 * business outside that window must use a pre-approved message template. A
 * production deployment would register an alert template with Meta; this
 * implementation sends plain text, which is enough for a demo number whose
 * recipients have opened a conversation first.
 */
@Component
public class WhatsAppSender {

    private final boolean enabled;
    private final String phoneNumberId;
    private final String accessToken;
    private final String apiVersion;
    private final RestTemplate restTemplate;

    public WhatsAppSender(
            @Value("${dpdms.alert.whatsapp.enabled:false}") boolean enabled,
            @Value("${dpdms.alert.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${dpdms.alert.whatsapp.access-token:}") String accessToken,
            @Value("${dpdms.alert.whatsapp.api-version:v20.0}") String apiVersion) {
        this.enabled = enabled;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
        this.apiVersion = apiVersion;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public DeliveryResult send(String toNumber, String text) {
        if (!enabled) {
            return DeliveryResult.simulated(
                    "WhatsApp channel is switched off (ALERT_WHATSAPP_ENABLED=false); nothing was sent");
        }
        if (toNumber == null) {
            return DeliveryResult.failed("No WhatsApp recipients configured (ALERT_WHATSAPP_RECIPIENTS)");
        }
        if (phoneNumberId.isBlank() || accessToken.isBlank()) {
            return DeliveryResult.failed(
                    "WhatsApp credentials missing (set WHATSAPP_PHONE_NUMBER_ID and WHATSAPP_ACCESS_TOKEN)");
        }

        String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", toNumber.replaceAll("[^0-9]", ""),
                "type", "text",
                "text", Map.of("body", truncate(text, 4000))
        );

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return DeliveryResult.sent("Accepted by the WhatsApp Cloud API");
        } catch (HttpStatusCodeException ex) {
            return DeliveryResult.failed("WhatsApp API returned " + ex.getStatusCode().value() + ": "
                    + truncate(ex.getResponseBodyAsString(), 500));
        } catch (Exception ex) {
            return DeliveryResult.failed("WhatsApp request failed: " + truncate(ex.getMessage(), 500));
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
