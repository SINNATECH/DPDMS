package zw.gov.dpdms.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zw.gov.dpdms.dashboard.exception.ForbiddenDashboardAccessException;
import zw.gov.dpdms.dashboard.security.DpdmsPrincipal;
import zw.gov.dpdms.dashboard.service.HazardAggregatorService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Approved-only, cross-hazard aggregation for the provincial dashboard")
public class DashboardController {

    private final HazardAggregatorService aggregatorService;

    @GetMapping("/summary")
    @Operation(summary = "Counts of approved incidents by hazard, severity, and status")
    public ResponseEntity<Map<String, Object>> summary(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        requireDashboardAccess(principal);
        return ResponseEntity.ok(aggregatorService.summary(bearerOnly(authHeader)));
    }

    @GetMapping("/recent")
    @Operation(summary = "Most recent approved incidents across all hazards")
    public ResponseEntity<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        requireDashboardAccess(principal);
        return ResponseEntity.ok(aggregatorService.recent(bearerOnly(authHeader), limit));
    }

    @GetMapping("/map")
    @Operation(summary = "Approved incident coordinates for map plotting")
    public ResponseEntity<List<Map<String, Object>>> map(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        requireDashboardAccess(principal);
        return ResponseEntity.ok(aggregatorService.mapPoints(bearerOnly(authHeader)));
    }

    private void requireDashboardAccess(DpdmsPrincipal principal) {
        if (!principal.isNational() && !principal.isAdmin()) {
            throw new ForbiddenDashboardAccessException(
                    "Only national or admin accounts may view the cross-hazard dashboard");
        }
    }

    private String bearerOnly(String authHeader) {
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}
