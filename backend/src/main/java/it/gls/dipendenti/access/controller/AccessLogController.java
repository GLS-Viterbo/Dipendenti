package it.gls.dipendenti.access.controller;

import io.micrometer.common.lang.Nullable;
import it.gls.dipendenti.access.dto.AccessAnomalyDTO;
import it.gls.dipendenti.access.exception.CardNotAssignedException;
import it.gls.dipendenti.access.exception.LogNotFoundException;
import it.gls.dipendenti.access.model.AccessLog;
import it.gls.dipendenti.access.service.AccessService;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.util.ErrorResponse;
import it.gls.dipendenti.util.Page;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/access-logs")
public class AccessLogController {

    private final AccessService accessService;

    public AccessLogController(AccessService accessService) {
        this.accessService = accessService;
    }

    @PostMapping("/read")
    public ResponseEntity<?> readCard(@RequestBody CardRequest cardRequest) {
        AccessService.DetailedAccessLog log = accessService.readCard(cardRequest.cardUid());
        if (log == null) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Card registered but not assigned to any employee");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @PostMapping
    public ResponseEntity<AccessLog> addManual(@RequestBody AccessLog log) {
        return ResponseEntity.ok(accessService.addManualLog(log));
    }



    @GetMapping
    public ResponseEntity<List<AccessLog>> getLogsInRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(required = false) LocalDate date) {

        if (start != null && end != null) {
            // ✅ Converti OffsetDateTime → Instant
            return ResponseEntity.ok(accessService.getLogsInTimeRange(
                    TimeZoneUtils.toInstant(start),
                    TimeZoneUtils.toInstant(end)
            ));
        }

        LocalDate queryDate = date != null ? date : TimeZoneUtils.todayCompanyDate();
        return ResponseEntity.ok(accessService.getLogsForDate(queryDate));
    }

    @GetMapping("/detailed/all")
    public ResponseEntity<List<AccessService.DetailedAccessLog>> getAllDetailedLogs(@RequestParam LocalDate date) {
        return ResponseEntity.ok(accessService.getDetailedLogsForDate(date));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AccessLog>> getLogsByEmployee(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        List<AccessLog> logs = accessService.getLogsInTimeRangeByEmployee(employeeId, start.toInstant(), end.toInstant());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/status/{employeeId}")
    public ResponseEntity<WorkStatus> getEmployeeStatus(@PathVariable Long employeeId) {
        return ResponseEntity.ok(new WorkStatus(accessService.isAtWork(employeeId)));
    }


    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLog(@PathVariable Long id, @RequestBody AccessLog accessLog) {
        AccessLog logToUpdate = new AccessLog(
                id,
                accessLog.employeeId(),
                accessLog.cardId(),
                accessLog.timestamp(),
                accessLog.type(),
                accessLog.modified(),
                accessLog.modifiedAt(),
                accessLog.deleted()
        );
        accessService.modifyLog(logToUpdate);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable Long id) {
        accessService.deleteLog(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/anomalies")
    public ResponseEntity<List<AccessAnomalyDTO>> getAllAnomalies(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<AccessAnomalyDTO> anomalies = accessService.findAllAnomalies(startDate, endDate);
        return ResponseEntity.ok(anomalies);
    }

    @GetMapping("/employee/{employeeId}/has-anomalies")
    public ResponseEntity<AnomalyCheckResponse> checkEmployeeAnomalies(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        boolean hasAnomalies = accessService.hasAnomaliesInRange(employeeId, startDate, endDate);
        return ResponseEntity.ok(new AnomalyCheckResponse(hasAnomalies));
    }

    @GetMapping("/count")
    public ResponseEntity<ActiveCount> getActiveCount() {
        return ResponseEntity.ok(
                new ActiveCount(accessService.getEmployeesWorking())
        );
    }



    @ExceptionHandler(LogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLogNotFound(LogNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Access log not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CardNotAssignedException.class)
    public ResponseEntity<ErrorResponse> handleCardNotAssigned(CardNotAssignedException ex) {
        ErrorResponse error = new ErrorResponse("Card not Assigned", HttpStatus.UNPROCESSABLE_ENTITY.value());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    public record CardRequest(
            String cardUid
    ) {}

    public record ActiveCount(Long count) {}
    public record WorkStatus(Boolean isWorking) {}
    public record AnomalyCheckResponse(boolean hasAnomalies) {}
}