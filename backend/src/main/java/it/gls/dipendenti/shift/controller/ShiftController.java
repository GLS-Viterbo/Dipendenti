package it.gls.dipendenti.shift.controller;

import it.gls.dipendenti.hr.exception.DuplicateGroupException;
import it.gls.dipendenti.hr.exception.GroupNotFoundException;
import it.gls.dipendenti.shift.exception.DuplicateShiftNameException;
import it.gls.dipendenti.shift.exception.ShiftNotFoundException;
import it.gls.dipendenti.shift.model.Shift;
import it.gls.dipendenti.shift.service.ShiftService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping
    public ResponseEntity<Shift> createShift(@RequestBody Shift shift) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftService.createShift(shift));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shift> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @GetMapping
    public ResponseEntity<List<Shift>> getAll() {
        return ResponseEntity.ok(shiftService.getAllShifts());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Shift>> getAllActive() {
        return ResponseEntity.ok(shiftService.getAllActiveShifts());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateShift(@PathVariable Long id, @RequestBody Shift shift) {
        shiftService.updateShift(id, shift);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long id) {
        shiftService.deleteShift(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DuplicateShiftNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateName(DuplicateShiftNameException ex) {
        ErrorResponse errorResponse = new ErrorResponse("There is already a shift with this name", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    }

    @ExceptionHandler(ShiftNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShiftNotFound(ShiftNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Shift not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }




}
