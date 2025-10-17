package it.gls.dipendenti.shift.controller;

import it.gls.dipendenti.absence.exception.OverlappingAbsenceException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.shift.exception.*;
import it.gls.dipendenti.shift.model.ShiftAssignment;
import it.gls.dipendenti.shift.service.ShiftAssignmentService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts/assignments")
public class ShiftAssignmentController {
    private final ShiftAssignmentService assignmentService;

    public ShiftAssignmentController(ShiftAssignmentService service) {
        this.assignmentService = service;
    }

    @PostMapping("/manual")
    public ResponseEntity<ShiftAssignment> createManualShiftAssignment(@RequestBody ShiftAssignment assignment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                assignmentService.createManualAssignment(assignment)
        );
    }

    @GetMapping
    public ResponseEntity<List<ShiftAssignment>> getAssignments(@RequestParam(required = false) LocalDate date,
                                                          @RequestParam(required = false) LocalDate startDate,
                                                          @RequestParam(required = false) LocalDate endDate) {
        if (date != null)
            return ResponseEntity.ok(assignmentService.getAssignmentsForDate(date));
        if (startDate != null && endDate != null)
            return ResponseEntity.ok(assignmentService.getAssignmentsInRange(startDate, endDate));
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftAssignment> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    @GetMapping("/employee/{id}")
    public ResponseEntity<List<ShiftAssignment>> getAssignmentByEmployee(@PathVariable Long id,
                                                                         @RequestParam(required = false) LocalDate date,
                                                                         @RequestParam(required = false) LocalDate startDate,
                                                                         @RequestParam(required = false) LocalDate endDate) {
        if(date != null)
            return ResponseEntity.ok(assignmentService.getEmployeeAssignmentsForDate(id, date));
        if(startDate != null && endDate != null)
            return ResponseEntity.ok(assignmentService.getEmployeeAssignmentsInRange(id, startDate, endDate));
        return ResponseEntity.ok(assignmentService.getEmployeeAssignments(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateAssignment(@PathVariable Long id, @RequestBody ShiftAssignment assignment) {
        assignmentService.updateAssignment(id, assignment);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/future/{id}")
    public ResponseEntity<Void> deleteFutureEmployeeAssignments(@PathVariable Long id) {
        assignmentService.deleteFutureEmployeeAssignments(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/generate")
    public ResponseEntity<Void> generateAssignments(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        assignmentService.generateAssignmentsForDateRange(startDate, endDate);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Employee not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    @ExceptionHandler(ShiftAssignmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssociationNotFound(ShiftAssignmentNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Assignment not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    @ExceptionHandler(OverlappingAbsenceException.class)
    public ResponseEntity<ErrorResponse> handleOverlappingAbsence(OverlappingAbsenceException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Assignment overlaps with absence", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    }

    @ExceptionHandler(OverlappingShiftException.class)
    public ResponseEntity<ErrorResponse> handleOverlappingShift(OverlappingShiftException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Assignment overlaps with another shift", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }




}
