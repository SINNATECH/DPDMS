package zw.gov.dpdms.alert.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zw.gov.dpdms.alert.dto.AlertEvaluationResult;
import zw.gov.dpdms.alert.dto.IncidentAlertRequest;
import zw.gov.dpdms.alert.exception.ForbiddenAlertAccessException;
import zw.gov.dpdms.alert.model.AlertChannel;
import zw.gov.dpdms.alert.repository.AlertLogRepository;
import zw.gov.dpdms.alert.rules.AlertRuleEngine;
import zw.gov.dpdms.alert.security.DpdmsPrincipal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertDispatcher dispatcher;

    @Mock
    private AlertLogRepository repository;

    private final AlertRuleEngine ruleEngine = new AlertRuleEngine(4.0, 50.0);

    private AlertService serviceWithRecipients;
    private AlertService serviceWithNoRecipients;

    @BeforeEach
    void setUp() {
        serviceWithRecipients = new AlertService(ruleEngine, dispatcher, repository,
                "one@example.com, two@example.com", "263771111111");
        serviceWithNoRecipients = new AlertService(ruleEngine, dispatcher, repository, "", "");
    }

    private IncidentAlertRequest floodRequest(double waterLevel) {
        Map<String, Object> incident = new HashMap<>();
        incident.put("id", 12);
        incident.put("ward", "Ward 3");
        incident.put("district", "Muzarabani");
        incident.put("severity", "CRITICAL");
        incident.put("peakWaterLevelMetres", waterLevel);

        IncidentAlertRequest request = new IncidentAlertRequest();
        request.setHazardType("FLOOD");
        request.setIncident(incident);
        return request;
    }

    @Test
    void qualifyingIncidentQueuesOneDeliveryPerRecipientPerChannel() {
        AlertEvaluationResult result = serviceWithRecipients.evaluate(floodRequest(5.2));

        assertThat(result.triggered()).isTrue();
        assertThat(result.deliveriesQueued()).isEqualTo(3);
        verify(dispatcher, times(2)).dispatch(eq(AlertChannel.EMAIL), anyString(), eq("FLOOD"),
                eq(12L), eq("Ward 3"), anyString(), anyString());
        verify(dispatcher, times(1)).dispatch(eq(AlertChannel.WHATSAPP), eq("263771111111"), eq("FLOOD"),
                eq(12L), eq("Ward 3"), anyString(), anyString());
    }

    @Test
    void nonQualifyingIncidentDispatchesNothing() {
        AlertEvaluationResult result = serviceWithRecipients.evaluate(floodRequest(2.0));

        assertThat(result.triggered()).isFalse();
        assertThat(result.deliveriesQueued()).isZero();
        verify(dispatcher, never()).dispatch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void withNoRecipientsConfiguredEachChannelIsStillLoggedOnce() {
        AlertEvaluationResult result = serviceWithNoRecipients.evaluate(floodRequest(5.2));

        assertThat(result.deliveriesQueued()).isEqualTo(2);
        verify(dispatcher).dispatch(eq(AlertChannel.EMAIL), isNull(), any(), any(), any(), any(), any());
        verify(dispatcher).dispatch(eq(AlertChannel.WHATSAPP), isNull(), any(), any(), any(), any(), any());
    }

    @Test
    void recorderCannotViewTheAlertLog() {
        DpdmsPrincipal recorder = new DpdmsPrincipal("r1", DpdmsPrincipal.Role.RECORDER, "FLOOD", "Ward 3");

        assertThatThrownBy(() -> serviceWithRecipients.logs(recorder, null))
                .isInstanceOf(ForbiddenAlertAccessException.class);
    }

    @Test
    void supervisorCannotViewAnotherHazardsAlerts() {
        DpdmsPrincipal supervisor = new DpdmsPrincipal("s1", DpdmsPrincipal.Role.SUPERVISOR, "FLOOD", null);

        assertThatThrownBy(() -> serviceWithRecipients.logs(supervisor, "FIRE"))
                .isInstanceOf(ForbiddenAlertAccessException.class);
    }

    @Test
    void supervisorSeesOnlyTheirOwnHazardsAlerts() {
        DpdmsPrincipal supervisor = new DpdmsPrincipal("s1", DpdmsPrincipal.Role.SUPERVISOR, "FLOOD", null);
        when(repository.findTop200ByHazardTypeOrderByCreatedAtDesc("FLOOD")).thenReturn(List.of());

        assertThat(serviceWithRecipients.logs(supervisor, null)).isEmpty();
        verify(repository).findTop200ByHazardTypeOrderByCreatedAtDesc("FLOOD");
        verify(repository, never()).findTop200ByOrderByCreatedAtDesc();
    }

    @Test
    void nationalUserSeesEveryHazard() {
        DpdmsPrincipal national = new DpdmsPrincipal("n1", DpdmsPrincipal.Role.NATIONAL, null, null);
        when(repository.findTop200ByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(serviceWithRecipients.logs(national, null)).isEmpty();
        verify(repository).findTop200ByOrderByCreatedAtDesc();
    }
}
