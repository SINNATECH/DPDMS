package zw.gov.dpdms.zoonotic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.gov.dpdms.zoonotic.dto.ZoonoticDiseaseIncidentRequest;
import zw.gov.dpdms.zoonotic.dto.ZoonoticDiseaseIncidentResponse;
import zw.gov.dpdms.zoonotic.dto.ReviewDecisionRequest;
import zw.gov.dpdms.zoonotic.exception.ForbiddenHazardAccessException;
import zw.gov.dpdms.zoonotic.exception.IncidentNotFoundException;
import zw.gov.dpdms.zoonotic.exception.InvalidStateTransitionException;
import zw.gov.dpdms.zoonotic.model.ZoonoticDiseaseIncident;
import zw.gov.dpdms.zoonotic.model.IncidentStatus;
import zw.gov.dpdms.zoonotic.repository.ZoonoticDiseaseIncidentRepository;
import zw.gov.dpdms.zoonotic.security.DpdmsPrincipal;

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
public class ZoonoticDiseaseIncidentService {

    private static final String HAZARD = "ZOONOTIC_DISEASE";

    private final ZoonoticDiseaseIncidentRepository repository;

    @Transactional
    public ZoonoticDiseaseIncidentResponse create(ZoonoticDiseaseIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isRecorder() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a zoonotic disease recorder may create zoonotic disease incidents");
        }

        ZoonoticDiseaseIncident incident = ZoonoticDiseaseIncident.builder()
                .ward(principal.ward())            // taken from the token, never from the body
                .district(request.getDistrict())
                .province(request.getProvince())
                .occurredAt(request.getOccurredAt())
                .reportedBy(principal.username())
                .severity(request.getSeverity())
                .status(IncidentStatus.PENDING)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .pathogenName(request.getPathogenName())
                .animalSpeciesAffected(request.getAnimalSpeciesAffected())
                .confirmedHumanCases(request.getConfirmedHumanCases())
                .confirmedAnimalCases(request.getConfirmedAnimalCases())
                .classification(request.getClassification())
                .build();

        return ZoonoticDiseaseIncidentResponse.from(repository.save(incident));
    }

    @Transactional(readOnly = true)
    public ZoonoticDiseaseIncidentResponse getById(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        ZoonoticDiseaseIncident incident = findOrThrow(id);
        requireVisibility(incident, principal);
        return ZoonoticDiseaseIncidentResponse.from(incident);
    }

    /**
     * Listing rules: NATIONAL and SUPERVISOR see approved records for
     * reporting/dashboard purposes via {@code onlyApproved=true}; a
     * SUPERVISOR reviewing their queue instead calls this with
     * {@code onlyApproved=false} to see PENDING items too. A RECORDER
     * only ever sees their own ward regardless of the flag.
     */
    @Transactional(readOnly = true)
    public List<ZoonoticDiseaseIncidentResponse> list(DpdmsPrincipal principal, boolean onlyApproved) {
        requireHazardScope(principal);

        List<ZoonoticDiseaseIncident> incidents;
        if (principal.isRecorder()) {
            incidents = repository.findByWard(principal.ward());
        } else if (onlyApproved) {
            incidents = repository.findByStatus(IncidentStatus.APPROVED);
        } else {
            incidents = repository.findAll();
        }

        return incidents.stream().map(ZoonoticDiseaseIncidentResponse::from).toList();
    }

    @Transactional
    public ZoonoticDiseaseIncidentResponse update(Long id, ZoonoticDiseaseIncidentRequest request, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        ZoonoticDiseaseIncident incident = findOrThrow(id);

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
        incident.setPathogenName(request.getPathogenName());
        incident.setAnimalSpeciesAffected(request.getAnimalSpeciesAffected());
        incident.setConfirmedHumanCases(request.getConfirmedHumanCases());
        incident.setConfirmedAnimalCases(request.getConfirmedAnimalCases());
        incident.setClassification(request.getClassification());

        // A resubmission after corrections goes straight back to PENDING.
        if (incident.getStatus() == IncidentStatus.CORRECTIONS_REQUESTED) {
            incident.setStatus(IncidentStatus.PENDING);
        }

        return ZoonoticDiseaseIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public void delete(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        ZoonoticDiseaseIncident incident = findOrThrow(id);

        if (!principal.isAdmin() &&
                (!principal.isRecorder() || !incident.getWard().equals(principal.ward()))) {
            throw new ForbiddenHazardAccessException("You may only delete records in your own ward");
        }
        repository.delete(incident);
    }

    // ---- Approval workflow ----

    @Transactional
    public ZoonoticDiseaseIncidentResponse approve(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        ZoonoticDiseaseIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.APPROVED);
        applyReview(incident, decision, principal);
        return ZoonoticDiseaseIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public ZoonoticDiseaseIncidentResponse reject(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("reject");
        ZoonoticDiseaseIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.REJECTED);
        applyReview(incident, decision, principal);
        return ZoonoticDiseaseIncidentResponse.from(repository.save(incident));
    }

    @Transactional
    public ZoonoticDiseaseIncidentResponse requestCorrections(Long id, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        decision.requireCommentFor("request corrections on");
        ZoonoticDiseaseIncident incident = transitionGuard(id, principal);
        incident.setStatus(IncidentStatus.CORRECTIONS_REQUESTED);
        applyReview(incident, decision, principal);
        return ZoonoticDiseaseIncidentResponse.from(repository.save(incident));
    }

    // ---- helpers ----

    private ZoonoticDiseaseIncident transitionGuard(Long id, DpdmsPrincipal principal) {
        requireHazardScope(principal);
        if (!principal.isSupervisor() && !principal.isAdmin()) {
            throw new ForbiddenHazardAccessException("Only a zoonotic disease supervisor may review zoonotic disease incidents");
        }
        ZoonoticDiseaseIncident incident = findOrThrow(id);
        if (incident.getStatus() != IncidentStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Only PENDING records can be reviewed; this record is " + incident.getStatus());
        }
        return incident;
    }

    private void applyReview(ZoonoticDiseaseIncident incident, ReviewDecisionRequest decision, DpdmsPrincipal principal) {
        incident.setReviewedBy(principal.username());
        incident.setReviewedAt(LocalDateTime.now());
        incident.setReviewComment(decision.getComment());
    }

    private void requireVisibility(ZoonoticDiseaseIncident incident, DpdmsPrincipal principal) {
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

    private ZoonoticDiseaseIncident findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }
}
