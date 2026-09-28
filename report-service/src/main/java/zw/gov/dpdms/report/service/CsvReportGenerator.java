package zw.gov.dpdms.report.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class CsvReportGenerator {

    public byte[] generate(List<Map<String, Object>> incidents) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", ReportRowMapper.HEADER)).append("\n");

        for (String[] row : ReportRowMapper.toRows(incidents)) {
            for (int i = 0; i < row.length; i++) {
                sb.append(escape(row[i]));
                sb.append(i < row.length - 1 ? "," : "\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Wraps a field in quotes and escapes embedded quotes if it contains a comma, quote, or newline. */
    private String escape(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }
}
