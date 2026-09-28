package zw.gov.dpdms.alert.sender;

import zw.gov.dpdms.alert.model.DeliveryStatus;

public record DeliveryResult(DeliveryStatus status, String detail) {

    public static DeliveryResult sent(String detail) {
        return new DeliveryResult(DeliveryStatus.SENT, detail);
    }

    public static DeliveryResult failed(String detail) {
        return new DeliveryResult(DeliveryStatus.FAILED, detail);
    }

    public static DeliveryResult simulated(String detail) {
        return new DeliveryResult(DeliveryStatus.SIMULATED, detail);
    }
}
