package zw.gov.dpdms.drought.dto;

import lombok.Builder;
import lombok.Getter;
import zw.gov.dpdms.drought.model.DroughtIncident;
import zw.gov.dpdms.drought.model.IncidentStatus;
import zw.gov.dpdms.drought.model.Severity;

import java.time.LocalDateTime;

@Getter
@Builder
public class DroughtIncidentResponse {

    private Long id;
    private String ward;
    private String district;
    private String province;
    private LocalDateTime occurredAt;
    private String reportedBy;
    private Severity severity;
    private IncidentStatus status;
    private Double latitude;
    private Double longitude;
    private Double rainfallDeficitMm;
    private Integer consecutiveDryDays;
    private Double cropFailurePercentage;
    private Integer peopleFacingWaterShortages;
    private Integer livestockMortalityCount;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DroughtIncidentResponse from(DroughtIncident e) {
        return DroughtIncidentResponse.builder()
                .id(e.getId())
                .ward(e.getWard())
                .district(e.getDistrict())
                .province(e.getProvince())
                .occurredAt(e.getOccurredAt())
                .reportedBy(e.getReportedBy())
                .severity(e.getSeverity())
                .status(e.getStatus())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .rainfallDeficitMm(e.getRainfallDeficitMm())
                .consecutiveDryDays(e.getConsecutiveDryDays())
                .cropFailurePercentage(e.getCropFailurePercentage())
                .peopleFacingWaterShortages(e.getPeopleFacingWaterShortages())
                .livestockMortalityCount(e.getLivestockMortalityCount())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .reviewComment(e.getReviewComment())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
