package it.gls.dipendenti.hr.service;

import it.gls.dipendenti.hr.exception.DeadlineNotFoundException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.EmployeeDeadline;
import it.gls.dipendenti.hr.repository.EmployeeDeadlineRepository;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeDeadlineService {

    private final EmployeeDeadlineRepository deadlineRepository;
    private final EmployeeRepository employeeRepository;

    public EmployeeDeadlineService(EmployeeDeadlineRepository deadlineRepository,
                                   EmployeeRepository employeeRepository) {
        this.deadlineRepository = deadlineRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Creates a new deadline for an employee
     * @param deadline deadline fields
     * @return deadline with generated id
     */
    @Transactional
    public EmployeeDeadline createDeadline(EmployeeDeadline deadline) {
        if (deadline.employeeId() == null)
            throw new IllegalArgumentException("Employee ID cannot be null");

        if(employeeRepository.findById(deadline.employeeId()).orElse(null) == null)
            throw new EmployeeNotFoundException();

        if (deadline.expirationDate() == null)
            throw new IllegalArgumentException("Expiration date cannot be null");

        if (deadline.type() == null || deadline.type().isBlank())
            throw new IllegalArgumentException("Deadline type cannot be null or empty");

        return deadlineRepository.save(deadline);
    }

    /**
     * Returns a deadline with given id
     * @param id id of the deadline
     * @return deadline
     */
    public EmployeeDeadline getDeadlineById(Long id) {
        return deadlineRepository.findById(id)
                .orElseThrow(DeadlineNotFoundException::new);
    }

    /**
     * Returns all deadlines for a specific employee
     * @param employeeId id of the employee
     * @return List of deadlines
     */
    public List<EmployeeDeadline> getEmployeeDeadlines(Long employeeId) {
        if (employeeId == null)
            throw new IllegalArgumentException("Employee ID cannot be null");

        return deadlineRepository.findByEmployeeId(employeeId);
    }

    /**
     * Returns all deadlines
     * @return List of all deadlines
     */
    public List<EmployeeDeadline> getAllDeadlines() {
        return deadlineRepository.findAll();
    }

    /**
     * Returns all active deadlines (not expired)
     * @return List of active deadlines
     */
    public List<EmployeeDeadline> getActiveDeadlines() {
        return deadlineRepository.findAllActive();
    }

    /**
     * Returns deadlines expiring within a certain number of days
     * @param days number of days to look ahead
     * @return List of upcoming deadlines
     */
    public List<EmployeeDeadline> getUpcomingDeadlines(int days) {

        if (days < 0)
            throw new IllegalArgumentException("Days must be positive");

        return deadlineRepository.findUpcoming(days);
    }

    /**
     * Returns deadlines that need notification
     * @return List of deadlines needing notification
     */
    public List<EmployeeDeadline> getDeadlinesNeedingNotification() {
        return deadlineRepository.findNeedingNotification();
    }

    /**
     * Modifies a deadline attributes
     * @param deadline the deadline to modify
     */
    @Transactional
    public void updateDeadline(EmployeeDeadline deadline) {
        if (deadline.id() == null)
            throw new IllegalArgumentException("Deadline ID cannot be null");

        if (deadline.employeeId() == null)
            throw new IllegalArgumentException("Employee ID cannot be null");

        if (deadline.expirationDate() == null)
            throw new IllegalArgumentException("Expiration date cannot be null");

        if (!deadlineRepository.update(deadline))
            throw new DeadlineNotFoundException();
    }

    /**
     * Marks a deadline as notified
     * @param deadlineId id of the deadline
     */
    @Transactional
    public void markDeadlineAsNotified(Long deadlineId) {
        if (deadlineId == null)
            throw new IllegalArgumentException("Deadline ID cannot be null");

        if (!deadlineRepository.markAsNotified(deadlineId))
            throw new DeadlineNotFoundException();
    }

    /**
     * Deletes a deadline
     * @param deadlineId id of the deadline to delete
     */
    @Transactional
    public void deleteDeadline(Long deadlineId) {
        if (deadlineId == null)
            throw new IllegalArgumentException("Deadline ID cannot be null");

        if (!deadlineRepository.delete(deadlineId))
            throw new DeadlineNotFoundException();
    }

    /**
     * Returns the number of deadlines for an employee
     * @param employeeId id of the employee
     * @return number of deadlines
     */
    public Long getDeadlineCountByEmployee(Long employeeId) {
        if (employeeId == null)
            throw new IllegalArgumentException("Employee ID cannot be null");

        return deadlineRepository.countByEmployeeId(employeeId);
    }
}