package it.gls.dipendenti.absence.controller;

import it.gls.dipendenti.absence.exception.DuplicateHolidayException;
import it.gls.dipendenti.absence.exception.HolidayNotFoundException;
import it.gls.dipendenti.absence.model.Holiday;
import it.gls.dipendenti.absence.service.HolidayService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    /**
     * Create a new holiday
     */
    @PostMapping
    public ResponseEntity<Holiday> createHoliday(@RequestBody Holiday holiday) {
        Holiday createdHoliday = holidayService.createHoliday(holiday);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdHoliday);
    }

    /**
     * Get holiday by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<Holiday> getHolidayById(@PathVariable Long id) {
        Holiday holiday = holidayService.getHolidayById(id);
        return ResponseEntity.ok(holiday);
    }

    /**
     * Get all holidays
     */
    @GetMapping
    public ResponseEntity<List<Holiday>> getAllHolidays(
            @RequestParam(required = false) Boolean recurring,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // Filter by date range
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(holidayService.getHolidaysByDateRange(startDate, endDate));
        }

        // Filter by recurring
        if (recurring != null && recurring) {
            return ResponseEntity.ok(holidayService.getRecurringHolidays());
        }

        // Return all
        return ResponseEntity.ok(holidayService.getAllHolidays());
    }

    /**
     * Check if a date is a holiday
     */
    @GetMapping("/check")
    public ResponseEntity<HolidayCheckResponse> checkHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean isHoliday = holidayService.isHoliday(date);
        return ResponseEntity.ok(new HolidayCheckResponse(isHoliday));
    }

    /**
     * Delete a holiday (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get count of holidays
     */
    @GetMapping("/count")
    public ResponseEntity<HolidayCountResponse> getHolidayCount() {

        List<Holiday> holidays =  holidayService.getAllHolidays();

        return ResponseEntity.ok(new HolidayCountResponse((long) holidays.size()));
    }

    // Exception Handlers

    @ExceptionHandler(HolidayNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleHolidayNotFound(HolidayNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateHolidayException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateHoliday(DuplicateHolidayException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // DTOs

    public record HolidayCheckResponse(boolean isHoliday) {}

    public record HolidayCountResponse(Long count) {}
}