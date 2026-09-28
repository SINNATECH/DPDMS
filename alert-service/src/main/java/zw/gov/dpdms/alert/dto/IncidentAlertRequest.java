package zw.gov.dpdms.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/** Sent by a hazard service after a supervisor approves an incident. */
@Getter
@Setter
public class IncidentAlertRequest {

    @NotBlank
    private String hazardType;

    @NotNull
    private Map<String, Object> incident;
}
