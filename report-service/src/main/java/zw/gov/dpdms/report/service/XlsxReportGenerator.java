package zw.gov.dpdms.report.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

@Component
public class XlsxReportGenerator {

    public byte[] generate(List<Map<String, Object>> incidents) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Incidents");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < ReportRowMapper.HEADER.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(ReportRowMapper.HEADER[col]);
                cell.setCellStyle(headerStyle);
            }

            List<String[]> rows = ReportRowMapper.toRows(incidents);
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                String[] values = rows.get(r);
                for (int col = 0; col < values.length; col++) {
                    row.createCell(col).setCellValue(values[col]);
                }
            }

            for (int col = 0; col < ReportRowMapper.HEADER.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate XLSX report", ex);
        }
    }
}
