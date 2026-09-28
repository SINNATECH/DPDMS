package zw.gov.dpdms.alert.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.gov.dpdms.alert.dto.AlertLogResponse;
import zw.gov.dpdms.alert.security.DpdmsPrincipal;
import zw.gov.dpdms.alert.service.AlertService;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert log", description = "Every alert delivery attempt: channel, recipient, timestamp, and status")
public class AlertLogController {

    private final AlertService alertService;

    @GetMapping("/logs")
    @Operation(summary = "The 200 most recent alert delivery attempts, newest first")
    public ResponseEntity<List<AlertLogResponse>> logs(
            @RequestParam(required = false) String hazard,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(alertService.logs(principal, hazard));
    }
}
