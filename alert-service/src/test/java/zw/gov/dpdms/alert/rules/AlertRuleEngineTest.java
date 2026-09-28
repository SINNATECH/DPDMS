package zw.gov.dpdms.alert.rules;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** One test per alerting criterion named in the assignment brief, plus the boundaries. */
class AlertRuleEngineTest {

    private final AlertRuleEngine engine = new AlertRuleEngine(4.0, 50.0);

    @Test
    void floodAtTheDangerThresholdTriggersAnAlert() {
        assertThat(engine.evaluate("FLOOD", Map.of("peakWaterLevelMetres", 4.0))).isPresent();
    }

    @Test
    void floodAboveTheDangerThresholdTriggersAnAlert() {
        assertThat(engine.evaluate("FLOOD", Map.of("peakWaterLevelMetres", 5.6))).isPresent();
    }

    @Test
    void floodBelowTheDangerThresholdDoesNot() {
        assertThat(engine.evaluate("FLOOD", Map.of("peakWaterLevelMetres", 3.9))).isEmpty();
    }

    @Test
    void fireThatIsStillActiveTriggersAnAlert() {
        assertThat(engine.evaluate("FIRE", Map.of("stillActive", true))).isPresent();
    }

    @Test
    void fireThatIsContainedDoesNot() {
        assertThat(engine.evaluate("FIRE", Map.of("stillActive", false))).isEmpty();
    }

    @Test
    void zoonoticClusterTriggersAnAlert() {
        assertThat(engine.evaluate("ZOONOTIC_DISEASE",
                Map.of("classification", "CLUSTER", "pathogenName", "Anthrax"))).isPresent();
    }

    @Test
    void zoonoticOutbreakTriggersAnAlert() {
        assertThat(engine.evaluate("ZOONOTIC_DISEASE", Map.of("classification", "OUTBREAK"))).isPresent();
    }

    @Test
    void zoonoticEventWithNoClassificationDoesNot() {
        assertThat(engine.evaluate("ZOONOTIC_DISEASE", Map.of("pathogenName", "Rabies"))).isEmpty();
    }

    @Test
    void miningAccidentWithFatalitiesTriggersAnAlert() {
        assertThat(engine.evaluate("MINING_ACCIDENT",
                Map.of("fatalities", 1, "trappedOrInjuredMiners", 0))).isPresent();
    }

    @Test
    void miningAccidentWithOnlyTrappedOrInjuredMinersTriggersAnAlert() {
        assertThat(engine.evaluate("MINING_ACCIDENT",
                Map.of("fatalities", 0, "trappedOrInjuredMiners", 3))).isPresent();
    }

    @Test
    void miningAccidentWithNoCasualtiesDoesNot() {
        assertThat(engine.evaluate("MINING_ACCIDENT",
                Map.of("fatalities", 0, "trappedOrInjuredMiners", 0))).isEmpty();
    }

    @Test
    void severeDroughtCropFailureTriggersAnAlert() {
        assertThat(engine.evaluate("DROUGHT", Map.of("cropFailurePercentage", 60.0))).isPresent();
    }

    @Test
    void mildDroughtCropFailureDoesNot() {
        assertThat(engine.evaluate("DROUGHT", Map.of("cropFailurePercentage", 20.0))).isEmpty();
    }

    @Test
    void hazardTypeIsCaseInsensitive() {
        assertThat(engine.evaluate("flood", Map.of("peakWaterLevelMetres", 5.0))).isPresent();
    }

    @Test
    void unknownHazardNeverTriggers() {
        assertThat(engine.evaluate("EARTHQUAKE", Map.of("magnitude", 9.0))).isEmpty();
    }

    @Test
    void missingIndicatorsAreHandledWithoutErrors() {
        assertThat(engine.evaluate("FLOOD", Map.of())).isEmpty();
    }
}
