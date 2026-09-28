package zw.gov.dpdms.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.gov.dpdms.report.dto.ReportFilter;
import zw.gov.dpdms.report.exception.UnsupportedReportFormatException;
import zw.gov.dpdms.report.security.DpdmsPrincipal;
import zw.gov.dpdms.report.service.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Generates filterable, approved-data-only reports across all five hazards")
public class ReportController {

    private final ReportDataService reportDataService;
    private final CsvReportGenerator csvGenerator;
    private final XlsxReportGenerator xlsxGenerator;
    private final DocxReportGenerator docxGenerator;
    private final PdfReportGenerator pdfGenerator;

    @GetMapping
    @Operation(summary = "Generate a report in csv, xlsx, docx, or pdf, filtered by hazard/ward/district/date/severity/status")
    public ResponseEntity<byte[]> generate(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) String hazard,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal DpdmsPrincipal principal) {

        ReportFilter filter = new ReportFilter(hazard, ward, district, startDate, endDate, severity, status);
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        List<Map<String, Object>> incidents = reportDataService.fetch(principal, filter, token);

        byte[] fileBytes;
        MediaType contentType;
        String extension;

        switch (format.toLowerCase()) {
            case "csv" -> {
                fileBytes = csvGenerator.generate(incidents);
                contentType = MediaType.parseMediaType("text/csv");
                extension = "csv";
            }
            case "xlsx" -> {
                fileBytes = xlsxGenerator.generate(incidents);
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                extension = "xlsx";
            }
            case "docx" -> {
                fileBytes = docxGenerator.generate(incidents);
                contentType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                extension = "docx";
            }
            case "pdf" -> {
                fileBytes = pdfGenerator.generate(incidents);
                contentType = MediaType.APPLICATION_PDF;
                extension = "pdf";
            }
            default -> throw new UnsupportedReportFormatException(format);
        }

        String filename = "dpdms-report-" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "." + extension;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fileBytes);
    }
}
