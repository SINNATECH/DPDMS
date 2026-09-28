package zw.gov.dpdms.alert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zw.gov.dpdms.alert.dto.AlertEvaluationResult;
import zw.gov.dpdms.alert.dto.IncidentAlertRequest;
import zw.gov.dpdms.alert.service.AlertService;

/**
 * Called by the hazard services (not by end users) after an incident is
 * approved. Returns 202 Accepted straight away: delivery continues in the
 * background, so the caller is never held up waiting on email or WhatsApp.
 */
@RestController
@RequestMapping("/internal/alerts")
@RequiredArgsConstructor
@Tag(name = "Internal alert intake", description = "Service-to-service only; requires the X-Internal-Key header")
public class InternalAlertController {

    private final AlertService alertService;

    @PostMapping("/evaluate")
    @Operation(summary = "Check an approved incident against the alert criteria and dispatch alerts if it qualifies")
    public ResponseEntity<AlertEvaluationResult> evaluate(@Valid @RequestBody IncidentAlertRequest request) {
        return ResponseEntity.accepted().body(alertService.evaluate(request));
    }
}
