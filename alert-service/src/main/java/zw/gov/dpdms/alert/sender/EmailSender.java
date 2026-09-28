package zw.gov.dpdms.alert.sender;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends alert emails through JavaMail (SMTP). SMTP credentials are read
 * from environment variables (MAIL_HOST, MAIL_PORT, MAIL_USERNAME,
 * MAIL_PASSWORD) via application.yml, never hardcoded.
 *
 * With ALERT_EMAIL_ENABLED=false (the default) nothing is sent and the
 * attempt is logged as SIMULATED.
 */
@Component
public class EmailSender {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean enabled;
    private final String from;

    public EmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${dpdms.alert.email.enabled:false}") boolean enabled,
            @Value("${dpdms.alert.email.from:alerts@dpdms.local}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.enabled = enabled;
        this.from = from;
    }

    public DeliveryResult send(String to, String subject, String body) {
        if (!enabled) {
            return DeliveryResult.simulated(
                    "Email channel is switched off (ALERT_EMAIL_ENABLED=false); nothing was sent");
        }
        if (to == null) {
            return DeliveryResult.failed("No email recipients configured (ALERT_EMAIL_RECIPIENTS)");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return DeliveryResult.failed("No mail server configured (set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD)");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return DeliveryResult.sent("Handed to the SMTP server");
        } catch (Exception ex) {
            return DeliveryResult.failed("SMTP error: " + truncate(ex.getMessage(), 500));
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) return "unknown error";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
