package zw.gov.dpdms.webclient.util;

import java.util.List;
import java.util.Map;

import static zw.gov.dpdms.webclient.util.FieldDef.*;

/**
 * Single source of truth for how each hazard maps to the gateway's API
 * path and which hazard-specific fields its create form needs. Keeping
 * this in one place means the list/create controllers and templates stay
 * generic instead of needing five near-duplicate copies.
 */
public final class HazardConfig {

    private HazardConfig() {}

    /** Maps the JWT "hazard" claim value to the gateway's URL path segment. */
    public static final Map<String, String> API_PATH = Map.of(
            "FLOOD", "floods",
            "DROUGHT", "droughts",
            "FIRE", "fires",
            "ZOONOTIC_DISEASE", "zoonotic-diseases",
            "MINING_ACCIDENT", "mining-accidents"
    );

    /** Human-readable label for headings/menus. */
    public static final Map<String, String> DISPLAY_NAME = Map.of(
            "FLOOD", "Flood",
            "DROUGHT", "Drought",
            "FIRE", "Fire",
            "ZOONOTIC_DISEASE", "Zoonotic Disease",
            "MINING_ACCIDENT", "Mining Accident"
    );

    /** The five hazard-specific fields each create form needs to collect. */
    public static final Map<String, List<FieldDef>> FIELDS = Map.of(
            "FLOOD", List.of(
                    decimal("peakWaterLevelMetres", "Peak water level (metres)"),
                    text("riverBasinName", "River basin / catchment name"),
                    integer("householdsDisplaced", "Households displaced"),
                    decimal("areaFloodedHectares", "Area flooded (hectares)"),
                    integer("inundationDurationDays", "Duration of inundation (days)")
            ),
            "DROUGHT", List.of(
                    decimal("rainfallDeficitMm", "Rainfall deficit (mm)"),
                    integer("consecutiveDryDays", "Consecutive dry days"),
                    decimal("cropFailurePercentage", "Crop failure (%)"),
                    integer("peopleFacingWaterShortages", "People facing water shortages"),
                    integer("livestockMortalityCount", "Livestock mortality count")
            ),
            "FIRE", List.of(
                    decimal("areaBurnedHectares", "Area burned (hectares)"),
                    select("suspectedCause", "Suspected cause",
                            List.of("NATURAL", "ACCIDENTAL", "DELIBERATE")),
                    integer("injuriesOrFatalities", "Injuries or fatalities"),
                    integer("structuresDestroyed", "Structures destroyed"),
                    checkbox("stillActive", "Fire still active")
            ),
            "ZOONOTIC_DISEASE", List.of(
                    text("pathogenName", "Pathogen / disease name"),
                    text("animalSpeciesAffected", "Animal species affected"),
                    integer("confirmedHumanCases", "Confirmed human cases"),
                    integer("confirmedAnimalCases", "Confirmed animal cases"),
                    select("classification", "Classification",
                            List.of("CLUSTER", "OUTBREAK"))
            ),
            "MINING_ACCIDENT", List.of(
                    text("mineName", "Mine name"),
                    select("mineType", "Mine type", List.of("FORMAL", "ARTISANAL")),
                    select("accidentType", "Accident type",
                            List.of("COLLAPSE", "GAS_EXPLOSION", "FLOODING", "FALL_OF_GROUND")),
                    integer("trappedOrInjuredMiners", "Trapped or injured miners"),
                    integer("fatalities", "Fatalities"),
                    checkbox("rescueOngoing", "Rescue operations ongoing")
            )
    );
}
