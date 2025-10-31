package it.gls.dipendenti.report.controller;

import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.report.model.MonthlyWorkingHoursStatsDTO;
import it.gls.dipendenti.report.service.StatsService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/employee/{employeeId}/monthly")
    public ResponseEntity<MonthlyWorkingHoursStatsDTO> getMonthlyStats(
            @PathVariable Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {

        MonthlyWorkingHoursStatsDTO stats = statsService.getMonthlyStats(employeeId, yearMonth);
        return ResponseEntity.ok(stats);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Employee not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}