package it.gls.dipendenti.access.controller;

import it.gls.dipendenti.access.exception.CardAlreadyAssigned;
import it.gls.dipendenti.access.exception.CardNotAssignedException;
import it.gls.dipendenti.access.model.CardAssignment;
import it.gls.dipendenti.access.service.CardAssignmentService;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final CardAssignmentService assignmentService;

    public AssignmentController(CardAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<CardAssignment> assignCard(@RequestBody CardAssignment cardAssignment) {
        CardAssignment newAssignment = assignmentService.assignCard(cardAssignment);
        return ResponseEntity.ok(newAssignment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeAssignment(@PathVariable Long id) {
        assignmentService.revokeAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<AssignedCount> getAssignedCardCount() {
        return ResponseEntity.ok(new AssignedCount(assignmentService.getCardAssignedCount()));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<CardAssignment>> getEmployeeAssignments(@PathVariable Long employeeId) {
        List<CardAssignment> assignments = assignmentService.getEmployeeAssignments(employeeId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/card/{cardId}/history")
    public ResponseEntity<List<CardAssignmentService.AssignmentHistoryRecord>> getCardHistory(@PathVariable Long cardId) {
        List<CardAssignmentService.AssignmentHistoryRecord> history = assignmentService.getCardAssignmentHistory(cardId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/card/{cardId}/employee")
    public ResponseEntity<Employee> getAssignedEmployee(@PathVariable Long cardId) {
        Employee employee = assignmentService.findAssignedEmployee(cardId);
        return ResponseEntity.ok(employee);
    }
    @GetMapping("/card/{cardId}")
    public ResponseEntity<CardAssignment> getCardAssignment(@PathVariable Long cardId) {
        CardAssignment assignment = assignmentService.getCardAssignment(cardId);
        if (assignment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(assignment);
    }

    @ExceptionHandler(CardAlreadyAssigned.class)
    public ResponseEntity<ErrorResponse> handleCardAlreadyAssigned(CardAlreadyAssigned ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CardNotAssignedException.class)
    public ResponseEntity<ErrorResponse> handleCardNotAssigned(CardNotAssignedException ex) {
        ErrorResponse error = new ErrorResponse("Card is not assigned or assignment not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    public record AssignedCount(Long count) {}
}
