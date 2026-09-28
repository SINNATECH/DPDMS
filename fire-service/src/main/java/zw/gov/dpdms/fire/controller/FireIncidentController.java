package zw.gov.dpdms.fire.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zw.gov.dpdms.fire.dto.FireIncidentRequest;
import zw.gov.dpdms.fire.dto.FireIncidentResponse;
import zw.gov.dpdms.fire.dto.ReviewDecisionRequest;
import zw.gov.dpdms.fire.security.DpdmsPrincipal;
import zw.gov.dpdms.fire.service.FireIncidentService;

import java.util.List;

@RestController
@RequestMapping("/api/fires")
@RequiredArgsConstructor
@Tag(name = "Fire incidents", description = "CRUD and approval workflow for fire hazard records")
public class FireIncidentController {

    private final FireIncidentService service;

    @PostMapping
    @Operation(summary = "Record a new fire incident (ward recorder only)")
    public ResponseEntity<FireIncidentResponse> create(
            @Valid @RequestBody FireIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single fire incident, subject to hazard/ward scoping")
    public ResponseEntity<FireIncidentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.getById(id, principal));
    }

    @GetMapping
    @Operation(summary = "List fire incidents visible to the caller")
    public ResponseEntity<List<FireIncidentResponse>> list(
            @RequestParam(defaultValue = "true") boolean onlyApproved,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.list(principal, onlyApproved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a PENDING or CORRECTIONS_REQUESTED record (own ward only)")
    public ResponseEntity<FireIncidentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FireIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a fire incident (own ward only)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        service.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ---- Approval workflow (provincial fire supervisor only) ----

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a PENDING fire incident (fire supervisor only)")
    public ResponseEntity<FireIncidentResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.approve(id, safe(decision), principal));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a PENDING fire incident with a reason (fire supervisor only)")
    public ResponseEntity<FireIncidentResponse> reject(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.reject(id, decision, principal));
    }

    @PostMapping("/{id}/request-corrections")
    @Operation(summary = "Send a PENDING fire incident back to the recorder for corrections")
    public ResponseEntity<FireIncidentResponse> requestCorrections(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.requestCorrections(id, decision, principal));
    }

    private ReviewDecisionRequest safe(ReviewDecisionRequest decision) {
        return decision != null ? decision : new ReviewDecisionRequest();
    }
}
