package it.gls.dipendenti.hr.service;

import it.gls.dipendenti.access.service.CardAssignmentService;
import it.gls.dipendenti.auth.model.CustomUserDetails;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.hr.exception.DuplicateEmployeeFound;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Contract;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.util.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final ContractService contractService;
    private final CardAssignmentService assignmentService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           ContractService contractService,
                           CardAssignmentService assignmentService) {
        this.employeeRepository = employeeRepository;
        this.contractService = contractService;
        this.assignmentService = assignmentService;
    }

    /**
     * Creates a new employee without starting contract
     * Cannot exist two employees with same tax code
     * @param employee employee fields
     * @return employee with generated id
     */
    @Transactional
    public Employee createEmployee(Employee employee) {
        if (employee.taxCode() == null || employee.taxCode().isBlank())
            throw new IllegalArgumentException("Tax code cannot be null or empty");

        if (employeeRepository.findByTaxCode(employee.taxCode()).isPresent())
            throw new DuplicateEmployeeFound("Employee with tax code %s already exists"
                    .formatted(employee.taxCode()));

        return employeeRepository.save(employee);
    }

    /**
     * Return count of active employees
     * @return active employees
     */
    public Long getActiveCount() {
        return employeeRepository.countActive(getCurrentUserCompanyId());
    }

    /**
     * Returns a list of employees without card assignments
     * @return list of employee
     */
    public List<Employee> getEmployeesWithoutCard() {
        return employeeRepository.getEmployeesWithoutCard(getCurrentUserCompanyId());
    }

    /**
     * Soft deletes an employee and invalidates his contract
     * @param employeeId id of the employee to delete
     */
    @Transactional
    public void deleteEmployee(Long employeeId) {
        if (employeeId == null)
            throw new IllegalArgumentException("Employee ID cannot be null");

        if (!employeeRepository.delete(employeeId))
            throw new EmployeeNotFoundException();

        // Invalidate contract if exists
        contractService.getEmployeeContract(employeeId)
                .ifPresent(c -> contractService.invalidateContract(c.id()));

        // Revoking all employee cards
        assignmentService.getEmployeeAssignments(employeeId)
                .forEach(ass -> assignmentService.revokeAssignment(ass.id()));

    }

    /**
     * Modifies an employee attributes
     * @param employee the employee to modify
     */
    @Transactional
    public void updateEmployee(Employee employee) {
        if (employee.id() == null)
            throw new IllegalArgumentException("Employee ID cannot be null");

        // Check if trying to change tax code to one already in use
        if (employee.taxCode() != null) {
            employeeRepository.findByTaxCode(employee.taxCode())
                    .ifPresent(existing -> {
                        if (!existing.id().equals(employee.id()))
                            throw new DuplicateEmployeeFound("Tax code already in use");
                    });
        }

        if (!employeeRepository.update(employee))
            throw new EmployeeNotFoundException();
    }

    /**
     * Returns an employee with given id
     * @param id id of the employee
     * @return employee
     */
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(EmployeeNotFoundException::new);
    }

    /**
     * Returns all active employees
     * @return List of employees
     */
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findAll(getCurrentUserCompanyId());
    }

    /**
     * Returns employees with pagination
     * @param page page number
     * @param size number of employees per page
     * @return a page of employees
     */
    public Page<Employee> getAllActiveEmployees(int page, int size) {
        return employeeRepository.findAll(page, size, getCurrentUserCompanyId());
    }

    /**
     * Get all deleted employees
     * @return all deleted employees
     */
    public List<Employee> getAllDeletedEmployees() {
        return employeeRepository.findAllDeleted();
    }

    private Long getCurrentUserCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getCompanyId();
    }
}
