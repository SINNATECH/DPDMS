package zw.gov.dpdms.flood.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import zw.gov.dpdms.flood.model.Severity;

import java.time.LocalDateTime;

/**
 * What a recorder submits. Deliberately excludes id, status, ward
 * (taken from the authenticated principal, not the client), and every
 * audit field — those are set server-side only.
 */
@Getter
@Setter
public class FloodIncidentRequest {

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

    @NotNull
    @PositiveOrZero
    private Double peakWaterLevelMetres;

    @NotBlank
    private String riverBasinName;

    @NotNull
    @PositiveOrZero
    private Integer householdsDisplaced;

    @NotNull
    @PositiveOrZero
    private Double areaFloodedHectares;

    @NotNull
    @PositiveOrZero
    private Integer inundationDurationDays;
}
