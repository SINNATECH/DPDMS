package zw.gov.dpdms.alert.model;

/**
 * SENT      - the provider accepted the message.
 * FAILED    - we tried and the provider (or our config) rejected it.
 * SIMULATED - the channel is switched off, so nothing left this machine.
 *             Recorded honestly instead of pretending a message was sent.
 */
public enum DeliveryStatus {
    SENT,
    FAILED,
    SIMULATED
}
