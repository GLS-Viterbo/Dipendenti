package it.gls.dipendenti.access.service;

import it.gls.dipendenti.access.exception.CardAlreadyAssigned;
import it.gls.dipendenti.access.exception.CardNotAssignedException;
import it.gls.dipendenti.access.exception.CardNotFoundException;
import it.gls.dipendenti.access.model.Card;
import it.gls.dipendenti.access.model.CardAssignment;
import it.gls.dipendenti.access.repository.CardAssignmentRepository;
import it.gls.dipendenti.access.repository.CardRepository;
import it.gls.dipendenti.auth.model.CustomUserDetails;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CardAssignmentService {

    private final CardAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CardService cardService;

    public CardAssignmentService(CardAssignmentRepository assignmentRepository, EmployeeRepository employeeRepository, CardService cardService) {
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
        this.cardService = cardService;
    }

    /**
     * Create a new assignment
     * @param cardAssignment the assignment to create
     * @return card assignment with new id
     */
    @Transactional
    public CardAssignment assignCard(CardAssignment cardAssignment) {
        if(cardAssignment.cardId() == null)
            throw new IllegalArgumentException("Card id is null");
        // Verifying that card exist
        if(!cardService.cardExistsById(cardAssignment.cardId()))
            throw new CardNotFoundException();
        // Verifying that card is not already assigned
        if(assignmentRepository.isAssigned(cardAssignment.cardId()))
            throw new CardAlreadyAssigned(cardAssignment.id());
        // Verifying that card is not deleted
        if(cardService.getById(cardAssignment.cardId()).deleted())
            throw new IllegalArgumentException("This card is deleted");
        // Verifying that employee exist
        if (cardAssignment.employeeId() == null)
            throw new IllegalArgumentException("Employee id is null");
        if(employeeRepository.findById(cardAssignment.employeeId()).isEmpty())
            throw new EmployeeNotFoundException();

        // Checking if card is already assigned
        if (assignmentRepository.isAssigned(cardAssignment.cardId()))
            throw new CardAlreadyAssigned(cardAssignment.cardId());
        // Setting start date to the day of the assignment and end date to null
        return assignmentRepository.save(new CardAssignment(null, cardAssignment.employeeId(),
                cardAssignment.cardId(), LocalDate.now(), null));
    }

    /**
     * Revokes a card assignment if card is assigned
     * @param assignmentId the id of the assignment
     */
    @Transactional
    public void revokeAssignment(Long assignmentId) {
        if (assignmentId == null)
            throw new IllegalArgumentException("Card id is null");
        if (!assignmentRepository.revokeNow(assignmentId))
            throw new CardNotAssignedException();
    }

    /**
     * Returns the employee assigned to the card
     * @param cardId id of the card
     * @return employee
     */
    @Transactional
    public Employee findAssignedEmployee(Long cardId) {
        if (cardId == null)
            throw new IllegalArgumentException("Card id is null");
        Long employeeId = assignmentRepository.getAssignedEmployeeId(cardId);
        return employeeRepository.findById(employeeId).orElseThrow(CardNotAssignedException::new);
    }

    /**
     * Return the assignment of a card based on its id
     * @param cardId id of the card
     * @return assignment of the card
     */
    @Transactional
    public CardAssignment getCardAssignment(Long cardId) {
        if (cardId == null)
            throw new IllegalArgumentException("Card id is null");
        return assignmentRepository.getActiveAssignmentByCard(cardId).orElseThrow(CardNotAssignedException::new);
    }

    /**
     * Returns the value of assigned cards
     * @return assigned cards
     */
    public Long getCardAssignedCount() {
        return assignmentRepository.getAssignedCards(getCurrentUserCompanyId());
    }

    /**
     * Returns all the card assigned to an employee
     * @param employeeId id of the employee
     * @return list of assignments
     */
    @Transactional
    public List<CardAssignment> getEmployeeAssignments(Long employeeId) {
        if (employeeId == null)
            throw new IllegalArgumentException("Employee id is null");
        return assignmentRepository.getActiveAssignmentsByEmployee(employeeId);
    }

    /**
     * Return all the previous assignments of a card
     * @param cardId id of the card
     * @return list of assignment with ended ones too
     */
    public List<AssignmentHistoryRecord> getCardAssignmentHistory(Long cardId) {
        if (cardId == null)
            throw new IllegalArgumentException("Card is is null");
        List<CardAssignment> records =  assignmentRepository.getHistoryByCard(cardId);
        List<AssignmentHistoryRecord> output = new ArrayList<>();

        for (CardAssignment record : records) {
            Employee recordEmployee = employeeRepository.findById(record.employeeId()).orElse(null);
            if (recordEmployee != null) {
                output.add(new AssignmentHistoryRecord(record.id(), recordEmployee.name(), recordEmployee.surname(), record.startDate(), record.endDate()));
            } else {
                output.add(new AssignmentHistoryRecord(record.id(), "SCONOSCIUTO", "", record.startDate(), record.endDate()));
            }
        }

        return output;
    }

    private Long getCurrentUserCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getCompanyId();
    }

    public record AssignmentHistoryRecord(
            Long id,
            String employeeName,
            String employeeSurname,
            LocalDate startDate,
            LocalDate endDate
    ) {}




}
