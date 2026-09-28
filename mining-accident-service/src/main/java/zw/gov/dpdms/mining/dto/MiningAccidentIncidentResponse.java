package zw.gov.dpdms.mining.dto;

import lombok.Builder;
import lombok.Getter;
import zw.gov.dpdms.mining.model.AccidentType;
import zw.gov.dpdms.mining.model.MineType;
import zw.gov.dpdms.mining.model.MiningAccidentIncident;
import zw.gov.dpdms.mining.model.IncidentStatus;
import zw.gov.dpdms.mining.model.Severity;

import java.time.LocalDateTime;

@Getter
@Builder
public class MiningAccidentIncidentResponse {

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
    private String mineName;
    private MineType mineType;
    private AccidentType accidentType;
    private Integer trappedOrInjuredMiners;
    private Integer fatalities;
    private Boolean rescueOngoing;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MiningAccidentIncidentResponse from(MiningAccidentIncident e) {
        return MiningAccidentIncidentResponse.builder()
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
                .mineName(e.getMineName())
                .mineType(e.getMineType())
                .accidentType(e.getAccidentType())
                .trappedOrInjuredMiners(e.getTrappedOrInjuredMiners())
                .fatalities(e.getFatalities())
                .rescueOngoing(e.getRescueOngoing())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .reviewComment(e.getReviewComment())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
