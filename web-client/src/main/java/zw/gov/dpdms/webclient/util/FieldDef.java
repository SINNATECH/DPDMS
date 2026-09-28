package zw.gov.dpdms.webclient.util;

import java.util.List;

/**
 * Describes one hazard-specific input field for the generic create-incident
 * form. type is one of "text", "integer", "decimal", "select", or
 * "checkbox" — integer vs decimal matters because the backend DTOs are
 * strict about it (an Integer field rejects a value like "40.0").
 * options is only used when type is "select".
 */
public record FieldDef(String name, String label, String type, List<String> options) {

    public static FieldDef text(String name, String label) {
        return new FieldDef(name, label, "text", null);
    }

    public static FieldDef integer(String name, String label) {
        return new FieldDef(name, label, "integer", null);
    }

    public static FieldDef decimal(String name, String label) {
        return new FieldDef(name, label, "decimal", null);
    }

    public static FieldDef select(String name, String label, List<String> options) {
        return new FieldDef(name, label, "select", options);
    }

    public static FieldDef checkbox(String name, String label) {
        return new FieldDef(name, label, "checkbox", null);
    }
}
