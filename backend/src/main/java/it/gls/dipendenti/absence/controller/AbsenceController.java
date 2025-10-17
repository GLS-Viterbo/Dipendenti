package it.gls.dipendenti.absence.controller;

import it.gls.dipendenti.absence.exception.AbsenceNotFoundException;
import it.gls.dipendenti.absence.exception.OverlappingAbsenceException;
import it.gls.dipendenti.absence.model.Absence;
import it.gls.dipendenti.absence.model.EmployeeLeaveAccrual;
import it.gls.dipendenti.absence.model.EmployeeLeaveBalance;
import it.gls.dipendenti.absence.repository.AbsenceRepository;
import it.gls.dipendenti.absence.service.AbsenceService;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/absences")
public class AbsenceController {

    private final AbsenceService absenceService;

    public AbsenceController(AbsenceService absenceService) {
        this.absenceService = absenceService;
    }

    @PostMapping("/init")
    public ResponseEntity<Void> initialize(@RequestBody EmployeeLeaveAccrual accrual) {
        absenceService.initializeEmployeeLeave(accrual);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Absence> createApprovedAbsence(@RequestBody Absence absence) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(absenceService.createApprovedAbsence(absence));
    }

    @GetMapping("/{employeeId}/balance")
    public ResponseEntity<EmployeeLeaveBalance> getEmployeeBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(absenceService.getEmployeeBalance(employeeId));
    }

    @GetMapping("/today")
    public ResponseEntity<AbsenceCount> getTodayCount() {
        return ResponseEntity.ok(
                new AbsenceCount(absenceService.getTodayCount())
        );
    }

    @GetMapping("/future")
    public ResponseEntity<AbsenceCount> getToApprove() {
        return ResponseEntity.ok(
                new AbsenceCount(absenceService.getFutureToApprove())
        );
    }


    @PutMapping("/{employeeId}/balance")
    public ResponseEntity<Void> updateBalance(@PathVariable Long employeeId, @RequestBody EmployeeLeaveBalance balance) {
        absenceService.updateBalance(employeeId, balance);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{employeeId}/accrual")
    public ResponseEntity<EmployeeLeaveAccrual> getEmployeeAccrual(@PathVariable Long employeeId) {
        return ResponseEntity.ok(absenceService.getEmployeeAccrual(employeeId));
    }

    @PutMapping("/{employeeId}/accrual")
    public ResponseEntity<Void> updateAccrual(@PathVariable Long employeeId, @RequestBody EmployeeLeaveAccrual accrual) {
        absenceService.updateAccrualRate(employeeId, accrual);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/negative")
    public ResponseEntity<List<EmployeeLeaveBalance>> getNegativeBalances() {
        return ResponseEntity.ok(absenceService.getNegativeBalances());
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<List<Absence>> getEmployeeAbsences(@PathVariable Long employeeId) {
        return ResponseEntity.ok(absenceService.getEmployeeAbsences(employeeId));
    }

    @GetMapping("")
    public ResponseEntity<List<Absence>> getAbsencesInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(absenceService.getAbsencesByDateRange(startDate, endDate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAbsence(@PathVariable Long id) {
        absenceService.deleteAbsence(id);
        return ResponseEntity.noContent().build();
    }


    @ExceptionHandler(AbsenceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AbsenceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(OverlappingAbsenceException.class)
    public ResponseEntity<ErrorResponse> handleOverlapping(OverlappingAbsenceException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.CONFLICT.value()));
    }
    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND.value()));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    public record AbsenceCount(Long count) {}





}
