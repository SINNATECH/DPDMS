package zw.gov.dpdms.flood.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zw.gov.dpdms.flood.dto.FloodIncidentRequest;
import zw.gov.dpdms.flood.dto.FloodIncidentResponse;
import zw.gov.dpdms.flood.dto.ReviewDecisionRequest;
import zw.gov.dpdms.flood.security.DpdmsPrincipal;
import zw.gov.dpdms.flood.service.FloodIncidentService;

import java.util.List;

@RestController
@RequestMapping("/api/floods")
@RequiredArgsConstructor
@Tag(name = "Flood incidents", description = "CRUD and approval workflow for flood hazard records")
public class FloodIncidentController {

    private final FloodIncidentService service;

    @PostMapping
    @Operation(summary = "Record a new flood incident (ward recorder only)")
    public ResponseEntity<FloodIncidentResponse> create(
            @Valid @RequestBody FloodIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single flood incident, subject to hazard/ward scoping")
    public ResponseEntity<FloodIncidentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.getById(id, principal));
    }

    @GetMapping
    @Operation(summary = "List flood incidents visible to the caller")
    public ResponseEntity<List<FloodIncidentResponse>> list(
            @RequestParam(defaultValue = "true") boolean onlyApproved,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.list(principal, onlyApproved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a PENDING or CORRECTIONS_REQUESTED record (own ward only)")
    public ResponseEntity<FloodIncidentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FloodIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a flood incident (own ward only)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        service.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ---- Approval workflow (provincial flood supervisor only) ----

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a PENDING flood incident (flood supervisor only)")
    public ResponseEntity<FloodIncidentResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.approve(id, safe(decision), principal));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a PENDING flood incident with a reason (flood supervisor only)")
    public ResponseEntity<FloodIncidentResponse> reject(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.reject(id, decision, principal));
    }

    @PostMapping("/{id}/request-corrections")
    @Operation(summary = "Send a PENDING flood incident back to the recorder for corrections")
    public ResponseEntity<FloodIncidentResponse> requestCorrections(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.requestCorrections(id, decision, principal));
    }

    private ReviewDecisionRequest safe(ReviewDecisionRequest decision) {
        return decision != null ? decision : new ReviewDecisionRequest();
    }
}
