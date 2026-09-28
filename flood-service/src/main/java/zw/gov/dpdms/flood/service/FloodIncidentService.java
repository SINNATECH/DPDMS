package zw.gov.dpdms.flood.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.gov.dpdms.flood.dto.FloodIncidentRequest;
import zw.gov.dpdms.flood.dto.FloodIncidentResponse;
import zw.gov.dpdms.flood.dto.ReviewDecisionRequest;
import zw.gov.dpdms.flood.exception.ForbiddenHazardAccessException;
import zw.gov.dpdms.flood.exception.IncidentNotFoundException;
import zw.gov.dpdms.flood.exception.InvalidStateTransitionException;
import zw.gov.dpdms.flood.model.FloodIncident;
import zw.gov.dpdms.flood.model.IncidentStatus;
import zw.gov.dpdms.flood.repository.FloodIncidentRepository;
import zw.gov.dpdms.flood.security.DpdmsPrincipal;

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
public class FloodIncidentService {

    private static final String HAZARD = "FLOOD";

    private final FloodIncidentRepository repository;

    @Transactional
    public FloodIncidentResponse create(FloodIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isRecorder() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a flood recorder may create flood incidents");
        }

        FloodIncident incident = FloodIncident.builder()
                .ward(principal.ward())            // taken from the token, never from the body
                .district(request.getDistrict())
                .province(request.getProvince())
                .occurredAt(request.getOccurredAt())
                .reportedBy(principal.username())
                .severity(request.getSeverity())
                .status(IncidentStatus.PENDING)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .peakWaterLevelMetres(request.getPeakWaterLevelMetres())
                .riverBasinName(request.getRiverBasinName())
                .householdsDisplaced(request.getHouseholdsDisplaced())
                .areaFloodedHectares(request.getAreaFloodedHectares())
                .inundationDurationDays(request.getInundationDurationDays())
                .build();

        return FloodIncidentResponse.from(repository.save(incident));
    }

    @Transactional(readOnly = true)
    public FloodIncidentResponse getById(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        FloodIncident incident = findOrThrow(id);
        requireVisibility(incident, principal);
        return FloodIncidentResponse.from(incident);
    }

    /**
     * Listing rules: NATIONAL and SUPERVISOR see approved records for
     * reporting/dashboard purposes via {@code onlyApproved=true}; a
     * SUPERVISOR reviewing their queue instead calls this with
     * {@code onlyApproved=false} to see PENDING items too. A RECORDER
     * only ever sees their own ward regardless of the flag.
     */
    @Transactional(readOnly = true)
    public List<FloodIncidentResponse> list(DpdmsPrincipal principal, boolean onlyApproved) {
        requireHazardScope(principal);

        List<FloodIncident> incidents;
        if (principal.isRecorder()) {
            incidents = repository.findByWard(principal.ward());
        } else if (onlyApproved) {
            incidents = repository.findByStatus(IncidentStatus.APPROVED);
        } else {
            incidents = repository.findAll();
        }

        return incidents.stream().map(FloodIncidentResponse::from).toList();
    }

    @Transactional
    public FloodIncidentResponse update(Long id, FloodIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        FloodIncident incident = findOrThrow(id);

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
        incident.setPeakWaterLevelMetres(request.getPeakWaterLevelMetres());
        incident.setRiverBasinName(request.getRiverBasinName());
        incident.setHouseholdsDisplaced(request.getHouseholdsDisplaced());
        incident.setAreaFloodedHectares(request.getAreaFloodedHectares());
        incident.setInundationDurationDays(request.getInundationDurationDays());

        // A resubmission after corrections goes straight back to PENDING.
        if (incident.getStatus() == IncidentStatus.CORRECTIONS_REQUESTED) {
            incident.setStatus(IncidentStatus.PENDING);
        }

        return FloodIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public void delete(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        FloodIncident incident = findOrThrow(id);

        if (!principal.isAdmin() &&
                (!principal.isRecorder() || !incident.getWard().equals(principal.ward()))) {
            throw new ForbiddenHazardAccessException("You may only delete records in your own ward");
        }
        repository.delete(incident);
    }

    // ---- Approval workflow ----

    @Transactional
    public FloodIncidentResponse approve(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        FloodIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.APPROVED);
        applyReview(incident, decision, principal);
        return FloodIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public FloodIncidentResponse reject(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("reject");
        FloodIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.REJECTED);
        applyReview(incident, decision, principal);
        return FloodIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public FloodIncidentResponse requestCorrections(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("request corrections on");
        FloodIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.CORRECTIONS_REQUESTED);
        applyReview(incident, decision, principal);
        return FloodIncidentResponse.from(repository.save(incident));
    }

    // ---- helpers ----

    private FloodIncident transitionGuard(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isSupervisor() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a flood supervisor may review flood incidents");
        }
        FloodIncident incident = findOrThrow(id);
        if (incident.getStatus() != IncidentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only PENDING records can be reviewed; this record is " + incident.getStatus());
        }
        return incident;
    }

    private void applyReview(FloodIncident incident, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        incident.setReviewedBy(principal.username());
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewComment(decision.getComment());
    }

    private void requireVisibility(FloodIncident incident, DpdmsPrincipal principal) {
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

    private FloodIncident findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }
}
