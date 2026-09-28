package zw.gov.dpdms.report.service;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class DocxReportGenerator {

    public byte[] generate(List<Map<String, Object>> incidents) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph title = doc.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setText("DPDMS Incident Report");
            titleRun.setBold(true);
            titleRun.setFontSize(16);

            XWPFParagraph meta = doc.createParagraph();
            XWPFRun metaRun = meta.createRun();
            metaRun.setText("Generated " + LocalDateTime.now() + " — " + incidents.size() + " record(s)");
            metaRun.setFontSize(9);
            metaRun.setColor("666666");

            doc.createParagraph(); // spacer

            List<String[]> rows = ReportRowMapper.toRows(incidents);
            XWPFTable table = doc.createTable(rows.size() + 1, ReportRowMapper.HEADER.length);
            table.setWidth("100%");

            XWPFTableRow headerRow = table.getRow(0);
            for (int col = 0; col < ReportRowMapper.HEADER.length; col++) {
                setCellBoldText(headerRow.getCell(col), ReportRowMapper.HEADER[col]);
            }

            for (int r = 0; r < rows.size(); r++) {
                XWPFTableRow row = table.getRow(r + 1);
                String[] values = rows.get(r);
                for (int col = 0; col < values.length; col++) {
                    row.getCell(col).setText(values[col]);
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate DOCX report", ex);
        }
    }

    private void setCellBoldText(XWPFTableCell cell, String text) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
    }
}
