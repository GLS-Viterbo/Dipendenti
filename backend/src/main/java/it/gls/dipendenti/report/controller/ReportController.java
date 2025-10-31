package it.gls.dipendenti.report.controller;

import it.gls.dipendenti.report.service.MonthlyReportService;
import it.gls.dipendenti.util.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

/**
 * Controller per la generazione di report mensili
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private final MonthlyReportService reportService;

    public ReportController(MonthlyReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Genera e scarica il report mensile in formato Excel
     * GET /api/reports/monthly/2025/01
     *
     * @param year Anno del report (es. 2025)
     * @param month Mese del report (1-12)
     * @return File Excel con il report
     */
    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<byte[]> downloadMonthlyReport(
            @PathVariable int year,
            @PathVariable int month) {

        try {
            logger.info("Richiesta generazione report per {}/{}", year, month);

            // Validazione input
            if (year < 2000 || year > 2100) {
                throw new IllegalArgumentException("Anno non valido: " + year);
            }
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Mese non valido: " + month);
            }

            YearMonth yearMonth = YearMonth.of(year, month);

            // Genera il report
            byte[] excelData = reportService.generateMonthlyReport(yearMonth);

            // Prepara gli headers per il download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(
                    "attachment",
                    String.format("report_dipendenti_%d_%02d.xlsx", year, month)
            );
            headers.setContentLength(excelData.length);

            logger.info("Report generato con successo ({} bytes)", excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (IllegalArgumentException e) {
            logger.error("Parametri non validi: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Errore durante la generazione del report", e);
            throw new RuntimeException("Impossibile generare il report", e);
        }
    }

    /**
     * Genera il report per il mese corrente
     * GET /api/reports/monthly/current
     */
    @GetMapping("/monthly/current")
    public ResponseEntity<byte[]> downloadCurrentMonthReport() {
        YearMonth currentMonth = YearMonth.now();
        return downloadMonthlyReport(currentMonth.getYear(), currentMonth.getMonthValue());
    }

    // ============= EXCEPTION HANDLERS =============

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse error = new ErrorResponse(
                "Errore durante la generazione del report: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ============= DTOs =============

    /**
     * Response con informazioni sul report
     */
    public record ReportInfoResponse(
            String period,
            String startDate,
            String endDate,
            String fileName,
            boolean available
    ) {}
}