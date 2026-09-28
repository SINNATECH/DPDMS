package zw.gov.dpdms.zoonotic.dto;

import lombok.Builder;
import lombok.Getter;
import zw.gov.dpdms.zoonotic.model.EventClassification;
import zw.gov.dpdms.zoonotic.model.ZoonoticDiseaseIncident;
import zw.gov.dpdms.zoonotic.model.IncidentStatus;
import zw.gov.dpdms.zoonotic.model.Severity;

import java.time.LocalDateTime;

@Getter
@Builder
public class ZoonoticDiseaseIncidentResponse {

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
    private String pathogenName;
    private String animalSpeciesAffected;
    private Integer confirmedHumanCases;
    private Integer confirmedAnimalCases;
    private EventClassification classification;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ZoonoticDiseaseIncidentResponse from(ZoonoticDiseaseIncident e) {
        return ZoonoticDiseaseIncidentResponse.builder()
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
                .pathogenName(e.getPathogenName())
                .animalSpeciesAffected(e.getAnimalSpeciesAffected())
                .confirmedHumanCases(e.getConfirmedHumanCases())
                .confirmedAnimalCases(e.getConfirmedAnimalCases())
                .classification(e.getClassification())
                .reviewedBy(e.getReviewedBy())
                .reviewedAt(e.getReviewedAt())
                .reviewComment(e.getReviewComment())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
