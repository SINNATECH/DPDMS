package zw.gov.dpdms.fire.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.gov.dpdms.fire.dto.FireIncidentRequest;
import zw.gov.dpdms.fire.dto.FireIncidentResponse;
import zw.gov.dpdms.fire.dto.ReviewDecisionRequest;
import zw.gov.dpdms.fire.exception.ForbiddenHazardAccessException;
import zw.gov.dpdms.fire.exception.IncidentNotFoundException;
import zw.gov.dpdms.fire.exception.InvalidStateTransitionException;
import zw.gov.dpdms.fire.model.FireIncident;
import zw.gov.dpdms.fire.model.IncidentStatus;
import zw.gov.dpdms.fire.repository.FireIncidentRepository;
import zw.gov.dpdms.fire.security.DpdmsPrincipal;

import java.time.LocalDateTime;
import java.util.List;

/**
 * All hazard-scoping and workflow rules live here, not in the controller.
 * The controller's only job is to pull the DpdmsPrincipal off the security
 * context and call through — every one of these methods is safe to call
 * even if a client tampers with the request body, because ward/hazard/role
 * checks always read from the trusted principal, never from client input.
 */
@Service
@RequiredArgsConstructor
public class FireIncidentService {

    private static final String HAZARD = "FIRE";

    private final FireIncidentRepository repository;

    @Transactional
    public FireIncidentResponse create(FireIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isRecorder() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a fire recorder may create fire incidents");
        }

        FireIncident incident = FireIncident.builder()
                .ward(principal.ward())            // taken from the token, never from the body
                .district(request.getDistrict())
                .province(request.getProvince())
                .occurredAt(request.getOccurredAt())
                .reportedBy(principal.username())
                .severity(request.getSeverity())
                .status(IncidentStatus.PENDING)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .areaBurnedHectares(request.getAreaBurnedHectares())
                .suspectedCause(request.getSuspectedCause())
                .injuriesOrFatalities(request.getInjuriesOrFatalities())
                .structuresDestroyed(request.getStructuresDestroyed())
                .stillActive(request.getStillActive())
                .build();

        return FireIncidentResponse.from(repository.save(incident));
    }

    @Transactional(readOnly = true)
    public FireIncidentResponse getById(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        FireIncident incident = findOrThrow(id);
        requireVisibility(incident, principal);
        return FireIncidentResponse.from(incident);
    }

    /**
     * Listing rules: NATIONAL and SUPERVISOR see approved records for
     * reporting/dashboard purposes via {@code onlyApproved=true}; a
     * SUPERVISOR reviewing their queue instead calls this with
     * {@code onlyApproved=false} to see PENDING items too. A RECORDER
     * only ever sees their own ward regardless of the flag.
     */
    @Transactional(readOnly = true)
    public List<FireIncidentResponse> list(DpdmsPrincipal principal, boolean onlyApproved) {
        requireHazardScope(principal);

        List<FireIncident> incidents;
        if (principal.isRecorder()) {
            incidents = repository.findByWard(principal.ward());
        } else if (onlyApproved) {
            incidents = repository.findByStatus(IncidentStatus.APPROVED);
        } else {
            incidents = repository.findAll();
        }

        return incidents.stream().map(FireIncidentResponse::from).toList();
    }

    @Transactional
    public FireIncidentResponse update(Long id, FireIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        FireIncident incident = findOrThrow(id);

        if (!principal.isAdmin()) {
            if (!principal.isRecorder() || !incident.getWard().equals(principal.ward())) {
                throw new ForbiddenHazardAccessException("You may only edit records in your own ward");
            }
            if (incident.getStatus() != IncidentStatus.PENDING
                    && incident.getStatus() != IncidentStatus.CORRECTIONS_REQUESTED) {
                throw new InvalidStateTransitionException(
                        "Only PENDING or CORRECTIONS_REQUESTED records may be edited");
            }
        }

        incident.setDistrict(request.getDistrict());
        incident.setProvince(request.getProvince());
        incident.setOccurredAt(request.getOccurredAt());
        incident.setSeverity(request.getSeverity());
        incident.setLatitude(request.getLatitude());
        incident.setLongitude(request.getLongitude());
        incident.setAreaBurnedHectares(request.getAreaBurnedHectares());
        incident.setSuspectedCause(request.getSuspectedCause());
        incident.setInjuriesOrFatalities(request.getInjuriesOrFatalities());
        incident.setStructuresDestroyed(request.getStructuresDestroyed());
        incident.setStillActive(request.getStillActive());

        // A resubmission after corrections goes straight back to PENDING.
        if (incident.getStatus() == IncidentStatus.CORRECTIONS_REQUESTED) {
            incident.setStatus(IncidentStatus.PENDING);
        }

        return FireIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public void delete(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        FireIncident incident = findOrThrow(id);

        if (!principal.isAdmin() &&
                (!principal.isRecorder() || !incident.getWard().equals(principal.ward()))) {
            throw new ForbiddenHazardAccessException("You may only delete records in your own ward");
        }
        repository.delete(incident);
    }

    // ---- Approval workflow ----

    @Transactional
    public FireIncidentResponse approve(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        FireIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.APPROVED);
        applyReview(incident, decision, principal);
        return FireIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public FireIncidentResponse reject(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("reject");
        FireIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.REJECTED);
        applyReview(incident, decision, principal);
        return FireIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public FireIncidentResponse requestCorrections(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("request corrections on");
        FireIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.CORRECTIONS_REQUESTED);
        applyReview(incident, decision, principal);
        return FireIncidentResponse.from(repository.save(incident));
    }

    // ---- helpers ----

    private FireIncident transitionGuard(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isSupervisor() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a fire supervisor may review fire incidents");
        }
        FireIncident incident = findOrThrow(id);
        if (incident.getStatus() != IncidentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only PENDING records can be reviewed; this record is " + incident.getStatus());
        }
        return incident;
    }

    private void applyReview(FireIncident incident, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        incident.setReviewedBy(principal.username());
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewComment(decision.getComment());
    }

    private void requireVisibility(FireIncident incident, DpdmsPrincipal principal) {
        if (principal.isAdmin() || principal.isSupervisor() || principal.isNational()) {
            if (principal.isNational() && incident.getStatus() != IncidentStatus.APPROVED) {
                throw new ForbiddenHazardAccessException("Pending records are not visible to national users");
            }
            return;
        }
        if (principal.isRecorder() && !incident.getWard().equals(principal.ward())) {
            throw new ForbiddenHazardAccessException("You may only view records in your own ward");
        }
    }

    private void requireHazardScope(DpdmsPrincipal principal) {
        if (!principal.isScopedToHazard(HAZARD)) {
            throw new ForbiddenHazardAccessException(
                    "This account is not authorised for the " + HAZARD + " hazard");
        }
    }

    private FireIncident findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }
}
