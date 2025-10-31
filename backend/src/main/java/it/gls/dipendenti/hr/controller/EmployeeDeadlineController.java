package it.gls.dipendenti.hr.controller;

import it.gls.dipendenti.hr.exception.DeadlineNotFoundException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.EmployeeDeadline;
import it.gls.dipendenti.hr.service.EmployeeDeadlineService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deadlines")
public class EmployeeDeadlineController {

    private final EmployeeDeadlineService deadlineService;

    public EmployeeDeadlineController(EmployeeDeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @PostMapping
    public ResponseEntity<EmployeeDeadline> createDeadline(@RequestBody EmployeeDeadline deadline) {
        EmployeeDeadline createdDeadline = deadlineService.createDeadline(deadline);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDeadline);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDeadline> getDeadlineById(@PathVariable Long id) {
        EmployeeDeadline deadline = deadlineService.getDeadlineById(id);
        return ResponseEntity.ok(deadline);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeDeadline>> getEmployeeDeadlines(@PathVariable Long employeeId) {
        List<EmployeeDeadline> deadlines = deadlineService.getEmployeeDeadlines(employeeId);
        return ResponseEntity.ok(deadlines);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDeadline>> getAllDeadlines(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(deadlineService.getActiveDeadlines());
        }
        return ResponseEntity.ok(deadlineService.getAllDeadlines());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EmployeeDeadline>> getUpcomingDeadlines(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        List<EmployeeDeadline> deadlines = deadlineService.getUpcomingDeadlines(days);
        return ResponseEntity.ok(deadlines);
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<EmployeeDeadline>> getDeadlinesNeedingNotification() {
        List<EmployeeDeadline> deadlines = deadlineService.getDeadlinesNeedingNotification();
        return ResponseEntity.ok(deadlines);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDeadline(@PathVariable Long id, @RequestBody EmployeeDeadline deadline) {
        deadlineService.updateDeadline(new EmployeeDeadline(
                id,
                deadline.employeeId(),
                deadline.type(),
                deadline.expirationDate(),
                deadline.note(),
                deadline.reminderDays(),
                deadline.recipientEmail(),
                deadline.notified()
        ));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/notify")
    public ResponseEntity<Void> markAsNotified(@PathVariable Long id) {
        deadlineService.markDeadlineAsNotified(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeadline(@PathVariable Long id) {
        deadlineService.deleteDeadline(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employee/{employeeId}/count")
    public ResponseEntity<DeadlineCountResponse> getDeadlineCount(@PathVariable Long employeeId) {
        Long count = deadlineService.getDeadlineCountByEmployee(employeeId);
        return ResponseEntity.ok(new DeadlineCountResponse(count));
    }

    @ExceptionHandler(DeadlineNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDeadlineNotFound(DeadlineNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Deadline not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Employee not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    public record DeadlineCountResponse(Long count) {}
}