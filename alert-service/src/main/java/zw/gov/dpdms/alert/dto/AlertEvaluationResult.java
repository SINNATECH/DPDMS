package zw.gov.dpdms.alert.dto;

public record AlertEvaluationResult(boolean triggered, String reason, int deliveriesQueued) {}
