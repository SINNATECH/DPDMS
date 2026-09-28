package zw.gov.dpdms.dashboard.util;

import java.util.List;
import java.util.Map;

public final class HazardRegistry {

    private HazardRegistry() {}

    public record HazardEndpoint(String serviceId, String apiPath, String displayName) {}

    public static final Map<String, HazardEndpoint> ENDPOINTS = Map.of(
            "FLOOD", new HazardEndpoint("flood-service", "floods", "Flood"),
            "DROUGHT", new HazardEndpoint("drought-service", "droughts", "Drought"),
            "FIRE", new HazardEndpoint("fire-service", "fires", "Fire"),
            "ZOONOTIC_DISEASE", new HazardEndpoint("zoonotic-disease-service", "zoonotic-diseases", "Zoonotic Disease"),
            "MINING_ACCIDENT", new HazardEndpoint("mining-accident-service", "mining-accidents", "Mining Accident")
    );

    public static final List<String> ALL_HAZARDS = List.copyOf(ENDPOINTS.keySet());
}
