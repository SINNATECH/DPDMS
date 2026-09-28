package zw.gov.dpdms.drought.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import zw.gov.dpdms.drought.alert.AlertNotifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zw.gov.dpdms.drought.dto.DroughtIncidentRequest;
import zw.gov.dpdms.drought.dto.DroughtIncidentResponse;
import zw.gov.dpdms.drought.dto.ReviewDecisionRequest;
import zw.gov.dpdms.drought.security.DpdmsPrincipal;
import zw.gov.dpdms.drought.service.DroughtIncidentService;

import java.util.List;

@RestController
@RequestMapping("/api/droughts")
@RequiredArgsConstructor
@Tag(name = "Drought incidents", description = "CRUD and approval workflow for drought hazard records")
public class DroughtIncidentController {

    private final DroughtIncidentService service;
    private final AlertNotifier alertNotifier;

    @PostMapping
    @Operation(summary = "Record a new drought incident (ward recorder only)")
    public ResponseEntity<DroughtIncidentResponse> create(
            @Valid @RequestBody DroughtIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single drought incident, subject to hazard/ward scoping")
    public ResponseEntity<DroughtIncidentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.getById(id, principal));
    }

    @GetMapping
    @Operation(summary = "List drought incidents visible to the caller")
    public ResponseEntity<List<DroughtIncidentResponse>> list(
            @RequestParam(defaultValue = "true") boolean onlyApproved,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.list(principal, onlyApproved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a PENDING or CORRECTIONS_REQUESTED record (own ward only)")
    public ResponseEntity<DroughtIncidentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DroughtIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a drought incident (own ward only)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        service.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ---- Approval workflow (provincial drought supervisor only) ----

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a PENDING drought incident (drought supervisor only)")
    public ResponseEntity<DroughtIncidentResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        DroughtIncidentResponse approved = service.approve(id, safe(decision), principal);
        // Runs only after the approval transaction has committed; fire-and-forget.
        alertNotifier.notifyApproved("DROUGHT", approved);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a PENDING drought incident with a reason (drought supervisor only)")
    public ResponseEntity<DroughtIncidentResponse> reject(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.reject(id, decision, principal));
    }

    @PostMapping("/{id}/request-corrections")
    @Operation(summary = "Send a PENDING drought incident back to the recorder for corrections")
    public ResponseEntity<DroughtIncidentResponse> requestCorrections(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.requestCorrections(id, decision, principal));
    }

    private ReviewDecisionRequest safe(ReviewDecisionRequest decision) {
        return decision != null ? decision : new ReviewDecisionRequest();
    }
}
