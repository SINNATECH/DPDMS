package zw.gov.dpdms.mining.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.gov.dpdms.mining.dto.MiningAccidentIncidentRequest;
import zw.gov.dpdms.mining.dto.MiningAccidentIncidentResponse;
import zw.gov.dpdms.mining.dto.ReviewDecisionRequest;
import zw.gov.dpdms.mining.exception.ForbiddenHazardAccessException;
import zw.gov.dpdms.mining.exception.IncidentNotFoundException;
import zw.gov.dpdms.mining.exception.InvalidStateTransitionException;
import zw.gov.dpdms.mining.model.MiningAccidentIncident;
import zw.gov.dpdms.mining.model.IncidentStatus;
import zw.gov.dpdms.mining.repository.MiningAccidentIncidentRepository;
import zw.gov.dpdms.mining.security.DpdmsPrincipal;

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
public class MiningAccidentIncidentService {

    private static final String HAZARD = "MINING_ACCIDENT";

    private final MiningAccidentIncidentRepository repository;

    @Transactional
    public MiningAccidentIncidentResponse create(MiningAccidentIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isRecorder() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a mining accident recorder may create mining accident incidents");
        }

        MiningAccidentIncident incident = MiningAccidentIncident.builder()
                .ward(principal.ward())            // taken from the token, never from the body
                .district(request.getDistrict())
                .province(request.getProvince())
                .occurredAt(request.getOccurredAt())
                .reportedBy(principal.username())
                .severity(request.getSeverity())
                .status(IncidentStatus.PENDING)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .mineName(request.getMineName())
                .mineType(request.getMineType())
                .accidentType(request.getAccidentType())
                .trappedOrInjuredMiners(request.getTrappedOrInjuredMiners())
                .fatalities(request.getFatalities())
                .rescueOngoing(request.getRescueOngoing())
                .build();

        return MiningAccidentIncidentResponse.from(repository.save(incident));
    }

    @Transactional(readOnly = true)
    public MiningAccidentIncidentResponse getById(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        MiningAccidentIncident incident = findOrThrow(id);
        requireVisibility(incident, principal);
        return MiningAccidentIncidentResponse.from(incident);
    }

    /**
     * Listing rules: NATIONAL and SUPERVISOR see approved records for
     * reporting/dashboard purposes via {@code onlyApproved=true}; a
     * SUPERVISOR reviewing their queue instead calls this with
     * {@code onlyApproved=false} to see PENDING items too. A RECORDER
     * only ever sees their own ward regardless of the flag.
     */
    @Transactional(readOnly = true)
    public List<MiningAccidentIncidentResponse> list(DpdmsPrincipal principal, boolean onlyApproved) {
        requireHazardScope(principal);

        List<MiningAccidentIncident> incidents;
        if (principal.isRecorder()) {
            incidents = repository.findByWard(principal.ward());
        } else if (onlyApproved) {
            incidents = repository.findByStatus(IncidentStatus.APPROVED);
        } else {
            incidents = repository.findAll();
        }

        return incidents.stream().map(MiningAccidentIncidentResponse::from).toList();
    }

    @Transactional
    public MiningAccidentIncidentResponse update(Long id, MiningAccidentIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        MiningAccidentIncident incident = findOrThrow(id);

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
        incident.setMineName(request.getMineName());
        incident.setMineType(request.getMineType());
        incident.setAccidentType(request.getAccidentType());
        incident.setTrappedOrInjuredMiners(request.getTrappedOrInjuredMiners());
        incident.setFatalities(request.getFatalities());
        incident.setRescueOngoing(request.getRescueOngoing());

        // A resubmission after corrections goes straight back to PENDING.
        if (incident.getStatus() == IncidentStatus.CORRECTIONS_REQUESTED) {
            incident.setStatus(IncidentStatus.PENDING);
        }

        return MiningAccidentIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public void delete(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        MiningAccidentIncident incident = findOrThrow(id);

        if (!principal.isAdmin() &&
                (!principal.isRecorder() || !incident.getWard().equals(principal.ward()))) {
            throw new ForbiddenHazardAccessException("You may only delete records in your own ward");
        }
        repository.delete(incident);
    }

    // ---- Approval workflow ----

    @Transactional
    public MiningAccidentIncidentResponse approve(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        MiningAccidentIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.APPROVED);
        applyReview(incident, decision, principal);
        return MiningAccidentIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public MiningAccidentIncidentResponse reject(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("reject");
        MiningAccidentIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.REJECTED);
        applyReview(incident, decision, principal);
        return MiningAccidentIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public MiningAccidentIncidentResponse requestCorrections(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("request corrections on");
        MiningAccidentIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.CORRECTIONS_REQUESTED);
        applyReview(incident, decision, principal);
        return MiningAccidentIncidentResponse.from(repository.save(incident));
    }

    // ---- helpers ----

    private MiningAccidentIncident transitionGuard(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isSupervisor() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a mining accident supervisor may review mining accident incidents");
        }
        MiningAccidentIncident incident = findOrThrow(id);
        if (incident.getStatus() != IncidentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only PENDING records can be reviewed; this record is " + incident.getStatus());
        }
        return incident;
    }

    private void applyReview(MiningAccidentIncident incident, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        incident.setReviewedBy(principal.username());
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewComment(decision.getComment());
    }

    private void requireVisibility(MiningAccidentIncident incident, DpdmsPrincipal principal) {
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

    private MiningAccidentIncident findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }
}
