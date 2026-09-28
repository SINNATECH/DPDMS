package zw.gov.dpdms.flood.dto;

import lombok.Builder;
import lombok.Getter;
import zw.gov.dpdms.flood.model.FloodIncident;
import zw.gov.dpdms.flood.model.IncidentStatus;
import zw.gov.dpdms.flood.model.Severity;

import java.time.LocalDateTime;

@Getter
@Builder
public class FloodIncidentResponse {

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
    private Double peakWaterLevelMetres;
    private String riverBasinName;
    private Integer householdsDisplaced;
    private Double areaFloodedHectares;
    private Integer inundationDurationDays;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FloodIncidentResponse from(FloodIncident e) {
        return FloodIncidentResponse.builder()
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
                .peakWaterLevelMetres(e.getPeakWaterLevelMetres())
                .riverBasinName(e.getRiverBasinName())
                .householdsDisplaced(e.getHouseholdsDisplaced())
                .areaFloodedHectares(e.getAreaFloodedHectares())
                .inundationDurationDays(e.getInundationDurationDays())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .reviewComment(e.getReviewComment())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
