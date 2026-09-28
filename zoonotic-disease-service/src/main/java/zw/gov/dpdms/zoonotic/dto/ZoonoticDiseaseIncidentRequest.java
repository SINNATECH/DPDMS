package zw.gov.dpdms.zoonotic.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import zw.gov.dpdms.zoonotic.model.EventClassification;
import zw.gov.dpdms.zoonotic.model.Severity;

import java.time.LocalDateTime;

/**
 * What a recorder submits. Deliberately excludes id, status, ward
 * (taken from the authenticated principal, not the client), and every
 * audit field — those are set server-side only.
 */
@Getter
@Setter
public class ZoonoticDiseaseIncidentRequest {

    @NotBlank
    private String district;

    @NotBlank
    private String province;

    @NotNull
    @PastOrPresent
    private LocalDateTime occurredAt;

    @NotNull
    private Severity severity;

    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    @NotBlank
    private String pathogenName;

    @NotBlank
    private String animalSpeciesAffected;

    @NotNull
    @PositiveOrZero
    private Integer confirmedHumanCases;

    @NotNull
    @PositiveOrZero
    private Integer confirmedAnimalCases;

    @NotNull
    private EventClassification classification;
}
