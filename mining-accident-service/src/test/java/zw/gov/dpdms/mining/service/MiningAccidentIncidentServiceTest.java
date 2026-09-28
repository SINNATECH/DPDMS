package zw.gov.dpdms.mining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.gov.dpdms.mining.dto.MiningAccidentIncidentRequest;
import zw.gov.dpdms.mining.dto.MiningAccidentIncidentResponse;
import zw.gov.dpdms.mining.dto.ReviewDecisionRequest;
import zw.gov.dpdms.mining.exception.ForbiddenHazardAccessException;
import zw.gov.dpdms.mining.exception.InvalidStateTransitionException;
import zw.gov.dpdms.mining.model.MiningAccidentIncident;
import zw.gov.dpdms.mining.model.IncidentStatus;
import zw.gov.dpdms.mining.model.Severity;
import zw.gov.dpdms.mining.repository.MiningAccidentIncidentRepository;
import zw.gov.dpdms.mining.security.DpdmsPrincipal;

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
class MiningAccidentIncidentServiceTest {

    @Mock
    private MiningAccidentIncidentRepository repository;

    @InjectMocks
    private MiningAccidentIncidentService service;

    private MiningAccidentIncidentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new MiningAccidentIncidentRequest();
        validRequest.setDistrict("Rushinga");
        validRequest.setProvince("Mashonaland Central");
        validRequest.setOccurredAt(LocalDateTime.now().minusDays(1));
        validRequest.setSeverity(Severity.HIGH);
        validRequest.setLatitude(-16.7);
        validRequest.setLongitude(32.1);
        validRequest.setMineName("Chiadzwa Diamond Mine");
        validRequest.setMineType(zw.gov.dpdms.mining.model.MineType.FORMAL);
        validRequest.setAccidentType(zw.gov.dpdms.mining.model.AccidentType.COLLAPSE);
        validRequest.setTrappedOrInjuredMiners(4);
        validRequest.setFatalities(1);
        validRequest.setRescueOngoing(true);
    }

    private DpdmsPrincipal recorder(String ward) {
        return new DpdmsPrincipal("recorder1", DpdmsPrincipal.Role.RECORDER, "MINING_ACCIDENT", ward);
    }

    private DpdmsPrincipal supervisor() {
        return new DpdmsPrincipal("supervisor1", DpdmsPrincipal.Role.SUPERVISOR, "MINING_ACCIDENT", null);
    }

    private DpdmsPrincipal wrongHazardRecorder(String ward) {
        return new DpdmsPrincipal("floodRecorder", DpdmsPrincipal.Role.RECORDER, "FLOOD", ward);
    }

    @Test
    void recorderCanCreateIncidentInOwnWard() {
        when(repository.save(any())).thenAnswer(inv -> {
            MiningAccidentIncident e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        MiningAccidentIncidentResponse response = service.create(validRequest, recorder("Ward 3"));

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
        MiningAccidentIncident pending = MiningAccidentIncident.builder()
                .id(5L).ward("Ward 3").status(IncidentStatus.PENDING).build();
        when(repository.findById(5L)).thenReturn(Optional.of(pending));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MiningAccidentIncidentResponse response = service.approve(5L, new ReviewDecisionRequest(), supervisor());

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.APPROVED);
    }

    @Test
    void cannotApproveAlreadyApprovedIncident() {
        MiningAccidentIncident approved = MiningAccidentIncident.builder()
                .id(6L).ward("Ward 3").status(IncidentStatus.APPROVED).build();
        when(repository.findById(6L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.approve(6L, new ReviewDecisionRequest(), supervisor()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void rejectingRequiresAComment() {
        MiningAccidentIncident pending = MiningAccidentIncident.builder()
                .id(7L).ward("Ward 3").status(IncidentStatus.PENDING).build();
        when(repository.findById(7L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.reject(7L, new ReviewDecisionRequest(), supervisor()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recorderCannotViewAnotherWardsIncident() {
        MiningAccidentIncident otherWard = MiningAccidentIncident.builder()
                .id(8L).ward("Ward 9").status(IncidentStatus.PENDING).build();
        when(repository.findById(8L)).thenReturn(Optional.of(otherWard));

        assertThatThrownBy(() -> service.getById(8L, recorder("Ward 3")))
                .isInstanceOf(ForbiddenHazardAccessException.class);
    }

    @Test
    void nationalUserCannotSeePendingRecords() {
        MiningAccidentIncident pending = MiningAccidentIncident.builder()
                .id(9L).ward("Ward 3").status(IncidentStatus.PENDING).build();
        when(repository.findById(9L)).thenReturn(Optional.of(pending));

        DpdmsPrincipal national = new DpdmsPrincipal("nat1", DpdmsPrincipal.Role.NATIONAL, null, null);

        assertThatThrownBy(() -> service.getById(9L, national))
                .isInstanceOf(ForbiddenHazardAccessException.class);
    }
}
