package zw.gov.dpdms.zoonotic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.gov.dpdms.zoonotic.dto.ZoonoticDiseaseIncidentRequest;
import zw.gov.dpdms.zoonotic.dto.ZoonoticDiseaseIncidentResponse;
import zw.gov.dpdms.zoonotic.dto.ReviewDecisionRequest;
import zw.gov.dpdms.zoonotic.exception.ForbiddenHazardAccessException;
import zw.gov.dpdms.zoonotic.exception.InvalidStateTransitionException;
import zw.gov.dpdms.zoonotic.model.ZoonoticDiseaseIncident;
import zw.gov.dpdms.zoonotic.model.IncidentStatus;
import zw.gov.dpdms.zoonotic.model.Severity;
import zw.gov.dpdms.zoonotic.repository.ZoonoticDiseaseIncidentRepository;
import zw.gov.dpdms.zoonotic.security.DpdmsPrincipal;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers exactly the two rule families the assignment brief calls out as
 * the most common source of bugs: hazard/ward scoping and the approval
 * state machine.
 */
@ExtendWith(MockitoExtension.class)
class ZoonoticDiseaseIncidentServiceTest {

    @Mock
    private ZoonoticDiseaseIncidentRepository repository;

    @InjectMocks
    private ZoonoticDiseaseIncidentService service;

    private ZoonoticDiseaseIncidentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ZoonoticDiseaseIncidentRequest();
        validRequest.setDistrict("Rushinga");
        validRequest.setProvince("Mashonaland Central");
        validRequest.setOccurredAt(LocalDateTime.now().minusDays(1));
        validRequest.setSeverity(Severity.HIGH);
        validRequest.setLatitude(-16.7);
        validRequest.setLongitude(32.1);
        validRequest.setPathogenName("Anthrax");
        validRequest.setAnimalSpeciesAffected("Cattle");
        validRequest.setConfirmedHumanCases(2);
        validRequest.setConfirmedAnimalCases(15);
        validRequest.setClassification(zw.gov.dpdms.zoonotic.model.EventClassification.OUTBREAK);
    }

    private DpdmsPrincipal recorder(String ward) {
        return new DpdmsPrincipal("recorder1", DpdmsPrincipal.Role.RECORDER, "ZOONOTIC_DISEASE", ward);
    }

    private DpdmsPrincipal supervisor() {
        return new DpdmsPrincipal("supervisor1", DpdmsPrincipal.Role.SUPERVISOR, "ZOONOTIC_DISEASE", null);
    }

    private DpdmsPrincipal wrongHazardRecorder(String ward) {
        return new DpdmsPrincipal("floodRecorder", DpdmsPrincipal.Role.RECORDER, "FLOOD", ward);
    }

    @Test
    void recorderCanCreateIncidentInOwnWard() {
        when(repository.save(any())).thenAnswer(inv -> {
            ZoonoticDiseaseIncident e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        ZoonoticDiseaseIncidentResponse response = service.create(validRequest, recorder("Ward 3"));

        assertThat(response.getWard()).isEqualTo("Ward 3");
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.PENDING);
    }

    @Test
    void recorderFromAnotherHazardIsRejectedWith403() {
        assertThatThrownBy(() -> service.create(validRequest, wrongHazardRecorder("Ward 3")))
                .isInstanceOf(ForbiddenHazardAccessException.class);
    }

    @Test
    void supervisorCannotCreateIncidents() {
        assertThatThrownBy(() -> service.create(validRequest, supervisor()))
                .isInstanceOf(ForbiddenHazardAccessException.class);
    }

    @Test
    void supervisorCanApprovePendingIncident() {
        ZoonoticDiseaseIncident pending = ZoonoticDiseaseIncident.builder()
                .id(5L).ward("Ward 3").status(IncidentStatus.PENDING).build();
        when(repository.findById(5L)).thenReturn(Optional.of(pending));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ZoonoticDiseaseIncidentResponse response = service.approve(5L, new ReviewDecisionRequest(), supervisor());

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.APPROVED);
    }

    @Test
    void cannotApproveAlreadyApprovedIncident() {
        ZoonoticDiseaseIncident approved = ZoonoticDiseaseIncident.builder()
                .id(6L).ward("Ward 3").status(IncidentStatus.APPROVED).build();
        when(repository.findById(6L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.approve(6L, new ReviewDecisionRequest(), supervisor()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void rejectingRequiresAComment() {
        ZoonoticDiseaseIncident pending = ZoonoticDiseaseIncident.builder()
                .id(7L).ward("Ward 3").status(IncidentStatus.PENDING).build();
        when(repository.findById(7L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.reject(7L, new ReviewDecisionRequest(), supervisor()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recorderCannotViewAnotherWardsIncident() {
        ZoonoticDiseaseIncident otherWard = ZoonoticDiseaseIncident.builder()
                .id(8L).ward("Ward 9").status(IncidentStatus.PENDING).build();
        when(repository.findById(8L)).thenReturn(Optional.of(otherWard));

        assertThatThrownBy(() -> service.getById(8L, recorder("Ward 3")))
                .isInstanceOf(ForbiddenHazardAccessException.class);
    }

    @Test
    void nationalUserCannotSeePendingRecords() {
        ZoonoticDiseaseIncident pending = ZoonoticDiseaseIncident.builder()
                .id(9L).ward("Ward 3").status(IncidentStatus.PENDING).build();
        when(repository.findById(9L)).thenReturn(Optional.of(pending));

        DpdmsPrincipal national = new DpdmsPrincipal("nat1", DpdmsPrincipal.Role.NATIONAL, null, null);

        assertThatThrownBy(() -> service.getById(9L, national))
                .isInstanceOf(ForbiddenHazardAccessException.class);
    }
}
