package zw.gov.dpdms.alert.rules;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * The "defined alerting criteria" from the brief:
 *   - a flood above a danger threshold
 *   - a fire that is still burning
 *   - a zoonotic disease cluster
 *   - a mining accident with casualties
 * plus one of our own for drought (severe crop failure), since the brief
 * lists five hazards but only gives examples for four.
 *
 * evaluate() returns the human-readable reason when an alert should fire,
 * or empty when the incident does not warrant one.
 */
@Component
public class AlertRuleEngine {

    private final double floodDangerLevelMetres;
    private final double droughtCropFailurePercent;

    public AlertRuleEngine(
            @Value("${dpdms.alert.flood-danger-level-metres:4.0}") double floodDangerLevelMetres,
            @Value("${dpdms.alert.drought-crop-failure-percent:50}") double droughtCropFailurePercent) {
        this.floodDangerLevelMetres = floodDangerLevelMetres;
        this.droughtCropFailurePercent = droughtCropFailurePercent;
    }

    public Optional<String> evaluate(String hazardType, Map<String, Object> incident) {
        if (hazardType == null || incident == null) {
            return Optional.empty();
        }
        return switch (hazardType.toUpperCase()) {
            case "FLOOD" -> flood(incident);
            case "DROUGHT" -> drought(incident);
            case "FIRE" -> fire(incident);
            case "ZOONOTIC_DISEASE" -> zoonotic(incident);
            case "MINING_ACCIDENT" -> mining(incident);
            default -> Optional.empty();
        };
    }

    private Optional<String> flood(Map<String, Object> incident) {
        Double level = number(incident.get("peakWaterLevelMetres"));
        if (level != null && level >= floodDangerLevelMetres) {
            return Optional.of("Peak water level of " + level + " m is at or above the danger threshold of "
                    + floodDangerLevelMetres + " m");
        }
        return Optional.empty();
    }

    private Optional<String> drought(Map<String, Object> incident) {
        Double failure = number(incident.get("cropFailurePercentage"));
        if (failure != null && failure >= droughtCropFailurePercent) {
            return Optional.of("Crop failure of " + failure + "% is at or above the alert threshold of "
                    + droughtCropFailurePercent + "%");
        }
        return Optional.empty();
    }

    private Optional<String> fire(Map<String, Object> incident) {
        if (bool(incident.get("stillActive"))) {
            return Optional.of("Fire is still active and has not been contained");
        }
        return Optional.empty();
    }

    private Optional<String> zoonotic(Map<String, Object> incident) {
        Object classification = incident.get("classification");
        if (classification != null) {
            String value = classification.toString().toUpperCase();
            if (value.equals("CLUSTER") || value.equals("OUTBREAK")) {
                Object pathogen = incident.get("pathogenName");
                return Optional.of("Zoonotic disease event classified as " + value
                        + (pathogen != null ? " (" + pathogen + ")" : ""));
            }
        }
        return Optional.empty();
    }

    private Optional<String> mining(Map<String, Object> incident) {
        Double fatalities = number(incident.get("fatalities"));
        Double trapped = number(incident.get("trappedOrInjuredMiners"));
        boolean hasFatalities = fatalities != null && fatalities > 0;
        boolean hasCasualties = trapped != null && trapped > 0;
        if (hasFatalities || hasCasualties) {
            return Optional.of("Mining accident with casualties: "
                    + (fatalities != null ? fatalities.intValue() : 0) + " fatalities, "
                    + (trapped != null ? trapped.intValue() : 0) + " trapped or injured");
        }
        return Optional.empty();
    }

    private static Double number(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value instanceof String s && s.trim().equalsIgnoreCase("true");
    }
}
