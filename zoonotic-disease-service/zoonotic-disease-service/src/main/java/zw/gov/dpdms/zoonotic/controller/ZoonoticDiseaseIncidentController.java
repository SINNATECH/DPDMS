package zw.gov.dpdms.zoonotic.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import zw.gov.dpdms.zoonotic.alert.AlertNotifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import zw.gov.dpdms.zoonotic.dto.ZoonoticDiseaseIncidentRequest;
import zw.gov.dpdms.zoonotic.dto.ZoonoticDiseaseIncidentResponse;
import zw.gov.dpdms.zoonotic.dto.ReviewDecisionRequest;
import zw.gov.dpdms.zoonotic.security.DpdmsPrincipal;
import zw.gov.dpdms.zoonotic.service.ZoonoticDiseaseIncidentService;

import java.util.List;

@RestController
@RequestMapping("/api/zoonotic-diseases")
@RequiredArgsConstructor
@Tag(name = "Zoonotic disease incidents", description = "CRUD and approval workflow for zoonotic disease hazard records")
public class ZoonoticDiseaseIncidentController {

    private final ZoonoticDiseaseIncidentService service;
    private final AlertNotifier alertNotifier;

    @PostMapping
    @Operation(summary = "Record a new zoonotic disease incident (ward recorder only)")
    public ResponseEntity<ZoonoticDiseaseIncidentResponse> create(
            @Valid @RequestBody ZoonoticDiseaseIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single zoonotic disease incident, subject to hazard/ward scoping")
    public ResponseEntity<ZoonoticDiseaseIncidentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.getById(id, principal));
    }

    @GetMapping
    @Operation(summary = "List zoonotic disease incidents visible to the caller")
    public ResponseEntity<List<ZoonoticDiseaseIncidentResponse>> list(
            @RequestParam(defaultValue = "true") boolean onlyApproved,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.list(principal, onlyApproved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a PENDING or CORRECTIONS_REQUESTED record (own ward only)")
    public ResponseEntity<ZoonoticDiseaseIncidentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ZoonoticDiseaseIncidentRequest request,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.update(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a zoonotic disease incident (own ward only)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        service.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ---- Approval workflow (provincial zoonotic disease supervisor only) ----

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a PENDING zoonotic disease incident (zoonotic disease supervisor only)")
    public ResponseEntity<ZoonoticDiseaseIncidentResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        ZoonoticDiseaseIncidentResponse approved = service.approve(id, safe(decision), principal);
        // Runs only after the approval transaction has committed; fire-and-forget.
        alertNotifier.notifyApproved("ZOONOTIC_DISEASE", approved);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a PENDING zoonotic disease incident with a reason (zoonotic disease supervisor only)")
    public ResponseEntity<ZoonoticDiseaseIncidentResponse> reject(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.reject(id, decision, principal));
    }

    @PostMapping("/{id}/request-corrections")
    @Operation(summary = "Send a PENDING zoonotic disease incident back to the recorder for corrections")
    public ResponseEntity<ZoonoticDiseaseIncidentResponse> requestCorrections(
            @PathVariable Long id,
            @RequestBody ReviewDecisionRequest decision,
            @AuthenticationPrincipal DpdmsPrincipal principal) {
        return ResponseEntity.ok(service.requestCorrections(id, decision, principal));
    }

    private ReviewDecisionRequest safe(ReviewDecisionRequest decision) {
        return decision != null ? decision : new ReviewDecisionRequest();
    }
}
