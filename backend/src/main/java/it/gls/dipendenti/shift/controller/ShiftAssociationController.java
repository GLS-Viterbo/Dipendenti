package it.gls.dipendenti.shift.controller;

import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.shift.exception.*;
import it.gls.dipendenti.shift.model.ShiftAssociation;
import it.gls.dipendenti.shift.service.ShiftAssociationService;
import it.gls.dipendenti.util.ErrorResponse;
import jakarta.websocket.server.PathParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts/associations")
public class ShiftAssociationController {
    private final ShiftAssociationService shiftAssociationService;

    public ShiftAssociationController(ShiftAssociationService shiftAssociationService) {
        this.shiftAssociationService = shiftAssociationService;
    }

    @PostMapping
    public ResponseEntity<ShiftAssociation> createAssociation(@RequestBody ShiftAssociationRequestDTO association) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftAssociationService.createAssociation(
                new ShiftAssociation(
                        null,
                        association.employeeId(),
                        association.shiftId(),
                        association.dayOfWeek()
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftAssociation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftAssociationService.getAssociationById(id));
    }

    @GetMapping("/employee/{id}")
    public ResponseEntity<List<ShiftAssociation>> getEmployeeAssociations(@PathVariable Long id, @RequestParam(required = false) Integer dayOfWeek) {
        if (dayOfWeek != null) {
            return ResponseEntity.ok(shiftAssociationService.getEmployeeAssociationForDay(id, DayOfWeek.of(dayOfWeek)));
        }
        return ResponseEntity.ok(shiftAssociationService.getEmployeeAssociations(id));
    }

    @GetMapping
    public ResponseEntity<List<ShiftAssociation>> getDayAssociations(@RequestParam(required = false) Integer dayOfWeek) {
        if(dayOfWeek != null)
            return ResponseEntity.ok(shiftAssociationService.getAssociationsForDay(DayOfWeek.of(dayOfWeek)));
        return ResponseEntity.ok(shiftAssociationService.getAllAssociations());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssociation(@PathVariable Long id) {
        shiftAssociationService.deleteAssociation(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("employee/{id}")
    public ResponseEntity<Void> deleteEmployeeAssociations(@PathVariable Long id) {
        shiftAssociationService.deleteEmployeeAssociations(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @ExceptionHandler(DuplicateShiftAssociationException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAssociation(DuplicateShiftAssociationException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Shift already associated to employee", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    }

    @ExceptionHandler(OverlappingShiftException.class)
    public ResponseEntity<ErrorResponse> handleOverlappingAssociation(OverlappingShiftException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Association overlap with another shift", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    }

    @ExceptionHandler(ShiftNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShiftNotFound(ShiftNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Shift not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Employee not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    @ExceptionHandler(InvalidShiftException.class)
    public ResponseEntity<ErrorResponse> handleInvaliShift(InvalidShiftException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

    }

    @ExceptionHandler(ShiftAssociationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssociationNotFound(ShiftAssociationNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Association not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    public record ShiftAssociationRequestDTO(
            Long employeeId,
            Long shiftId,
            Integer dayOfWeek
    ) {}
}
