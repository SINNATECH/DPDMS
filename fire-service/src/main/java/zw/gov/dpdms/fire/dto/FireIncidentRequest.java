package zw.gov.dpdms.fire.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import zw.gov.dpdms.fire.model.FireCause;
import zw.gov.dpdms.fire.model.Severity;

import java.time.LocalDateTime;

/**
 * What a recorder submits. Deliberately excludes id, status, ward
 * (taken from the authenticated principal, not the client), and every
 * audit field — those are set server-side only.
 */
@Getter
@Setter
public class FireIncidentRequest {

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
    private Double areaBurnedHectares;

    @NotNull
    private FireCause suspectedCause;

    @NotNull
    @PositiveOrZero
    private Integer injuriesOrFatalities;

    @NotNull
    @PositiveOrZero
    private Integer structuresDestroyed;

    @NotNull
    private Boolean stillActive;
}
