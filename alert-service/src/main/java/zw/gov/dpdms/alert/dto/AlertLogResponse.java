package zw.gov.dpdms.alert.dto;

import zw.gov.dpdms.alert.model.AlertChannel;
import zw.gov.dpdms.alert.model.AlertLog;
import zw.gov.dpdms.alert.model.DeliveryStatus;

import java.time.LocalDateTime;

public record AlertLogResponse(
        Long id,
        String hazardType,
        Long incidentId,
        String ward,
        AlertChannel channel,
        String recipient,
        String message,
        DeliveryStatus status,
        String detail,
        LocalDateTime createdAt
) {
    public static AlertLogResponse from(AlertLog log) {
        return new AlertLogResponse(
                log.getId(), log.getHazardType(), log.getIncidentId(), log.getWard(),
                log.getChannel(), log.getRecipient(), log.getMessage(),
                log.getStatus(), log.getDetail(), log.getCreatedAt());
    }
}
