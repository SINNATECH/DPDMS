package zw.gov.dpdms.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import zw.gov.dpdms.alert.model.AlertChannel;
import zw.gov.dpdms.alert.model.AlertLog;
import zw.gov.dpdms.alert.model.DeliveryStatus;
import zw.gov.dpdms.alert.repository.AlertLogRepository;
import zw.gov.dpdms.alert.sender.DeliveryResult;
import zw.gov.dpdms.alert.sender.EmailSender;
import zw.gov.dpdms.alert.sender.WhatsAppSender;

/**
 * Sends one alert to one recipient on one channel, then records the outcome.
 *
 * This is deliberately its own bean: Spring's @Async works through a proxy,
 * so it only takes effect when called from a different bean (AlertService).
 * Calling an @Async method from inside the same class would silently run
 * synchronously. Running on the "alertExecutor" pool means a slow SMTP server
 * or WhatsApp API can never hold up incident capture.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertDispatcher {

    private final EmailSender emailSender;
    private final WhatsAppSender whatsAppSender;
    private final AlertLogRepository repository;

    @Async("alertExecutor")
    public void dispatch(AlertChannel channel, String recipient, String hazardType,
                         Long incidentId, String ward, String subject, String body) {
        DeliveryResult result;
        try {
            result = switch (channel) {
                case EMAIL -> emailSender.send(recipient, subject, body);
                case WHATSAPP -> whatsAppSender.send(recipient, subject + "\n\n" + body);
            };
        } catch (Exception ex) {
            result = DeliveryResult.failed("Unexpected error: " + ex.getMessage());
        }

        try {
            repository.save(AlertLog.builder()
                    .hazardType(hazardType)
                    .incidentId(incidentId)
                    .ward(ward)
                    .channel(channel)
                    .recipient(recipient != null ? recipient : "(none configured)")
                    .message(truncate(subject + "\n\n" + body, 2000))
                    .status(result.status())
                    .detail(truncate(result.detail(), 1000))
                    .build());
        } catch (Exception ex) {
            log.error("Could not record alert log entry ({} via {}): {}", hazardType, channel, ex.getMessage());
        }

        if (result.status() == DeliveryStatus.FAILED) {
            log.warn("Alert delivery failed ({} via {}): {}", hazardType, channel, result.detail());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
