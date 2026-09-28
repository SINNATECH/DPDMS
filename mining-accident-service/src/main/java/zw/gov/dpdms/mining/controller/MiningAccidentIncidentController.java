package zw.gov.dpdms.mining.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zw.gov.dpdms.mining.dto.MiningAccidentIncidentRequest;
import zw.gov.dpdms.mining.dto.MiningAccidentIncidentResponse;
import zw.gov.dpdms.mining.dto.ReviewDecisionRequest;
import zw.gov.dpdms.mining.security.DpdmsPrincipal;
import zw.gov.dpdms.mining.service.MiningAccidentIncidentService;

import java.util.List;

@RestController
@RequestMapping("/api/mining-accidents")
@RequiredArgsConstructor
@Tag(name = "Mining accident incidents", description = "CRUD and approval workflow for mining accident hazard records")
public class MiningAccidentIncidentController {

    private final MiningAccidentIncidentService service;

    @PostMapping
    @Operation(summary = "Record a new mining accident incident (ward recorder only)")
    public ResponseEntity<MiningAccidentIncidentResponse> create(
            @Valid @RequestBody MiningAccidentIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single mining accident incident, subject to hazard/ward scoping")
    public ResponseEntity<MiningAccidentIncidentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.getById(id, principal));
    }

    @GetMapping
    @Operation(summary = "List mining accident incidents visible to the caller")
    public ResponseEntity<List<MiningAccidentIncidentResponse>> list(
            @RequestParam(defaultValue = "true") boolean onlyApproved,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.list(principal, onlyApproved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a PENDING or CORRECTIONS_REQUESTED record (own ward only)")
    public ResponseEntity<MiningAccidentIncidentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MiningAccidentIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a mining accident incident (own ward only)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        service.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ---- Approval workflow (provincial mining accident supervisor only) ----

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a PENDING mining accident incident (mining accident supervisor only)")
    public ResponseEntity<MiningAccidentIncidentResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.approve(id, safe(decision), principal));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a PENDING mining accident incident with a reason (mining accident supervisor only)")
    public ResponseEntity<MiningAccidentIncidentResponse> reject(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.reject(id, decision, principal));
    }

    @PostMapping("/{id}/request-corrections")
    @Operation(summary = "Send a PENDING mining accident incident back to the recorder for corrections")
    public ResponseEntity<MiningAccidentIncidentResponse> requestCorrections(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.requestCorrections(id, decision, principal));
    }

    private ReviewDecisionRequest safe(ReviewDecisionRequest decision) {
        return decision != null ? decision : new ReviewDecisionRequest();
    }
}
