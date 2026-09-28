package zw.gov.dpdms.fire.dto;

import lombok.Builder;
import lombok.Getter;
import zw.gov.dpdms.fire.model.FireCause;
import zw.gov.dpdms.fire.model.FireIncident;
import zw.gov.dpdms.fire.model.IncidentStatus;
import zw.gov.dpdms.fire.model.Severity;

import java.time.LocalDateTime;

@Getter
@Builder
public class FireIncidentResponse {

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
    private Double areaBurnedHectares;
    private FireCause suspectedCause;
    private Integer injuriesOrFatalities;
    private Integer structuresDestroyed;
    private Boolean stillActive;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FireIncidentResponse from(FireIncident e) {
        return FireIncidentResponse.builder()
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
                .areaBurnedHectares(e.getAreaBurnedHectares())
                .suspectedCause(e.getSuspectedCause())
                .injuriesOrFatalities(e.getInjuriesOrFatalities())
                .structuresDestroyed(e.getStructuresDestroyed())
                .stillActive(e.getStillActive())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .reviewComment(e.getReviewComment())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
