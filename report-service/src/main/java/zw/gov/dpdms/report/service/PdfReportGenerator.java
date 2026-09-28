package zw.gov.dpdms.report.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class PdfReportGenerator {

    public byte[] generate(List<Map<String, Object>> incidents) {
        // Landscape — 12 columns don't fit comfortably in portrait.
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

            document.add(new Paragraph("DPDMS Incident Report", titleFont));
            document.add(new Paragraph(
                    "Generated " + LocalDateTime.now() + " — " + incidents.size() + " record(s)", metaFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(ReportRowMapper.HEADER.length);
            table.setWidthPercentage(100);

            for (String heading : ReportRowMapper.HEADER) {
                PdfPCell cell = new PdfPCell(new Phrase(heading, headerFont));
                cell.setBackgroundColor(new Color(230, 230, 230));
                table.addCell(cell);
            }

            for (String[] row : ReportRowMapper.toRows(incidents)) {
                for (String value : row) {
                    table.addCell(new PdfPCell(new Phrase(value == null ? "" : value, cellFont)));
                }
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (DocumentException ex) {
            throw new RuntimeException("Failed to generate PDF report", ex);
        }
    }
}
