package it.gls.dipendenti.hr.controller;

import it.gls.dipendenti.hr.exception.*;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.model.Group;
import it.gls.dipendenti.hr.service.EmployeeService;
import it.gls.dipendenti.hr.service.GroupService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("api/employees")
public class EmployeeController {

    private EmployeeService employeeService;
    private GroupService groupService;

    public EmployeeController(EmployeeService employeeService, GroupService groupService) {
        this.employeeService = employeeService;
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        Employee createdEmployee = employeeService.createEmployee(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        employeeService.updateEmployee(new Employee(
                id,
                employee.companyId(),
                employee.name(),
                employee.surname(),
                employee.taxCode(),
                employee.birthday(),
                employee.address(),
                employee.city(),
                employee.email(),
                employee.phone(),
                employee.note(),
                employee.deleted()
        ));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Employee employee = employeeService.getEmployeeById(id);
        return ResponseEntity.status(HttpStatus.OK).body(employee);
    }

    @GetMapping("/count")
    public ResponseEntity<EmployeeCount> countActiveEmployees() {
        EmployeeCount count = new EmployeeCount(employeeService.getActiveCount());
        return ResponseEntity.ok(count);
    }

    @GetMapping("/without-card")
    public ResponseEntity<List<Employee>> getEmployeesWithoutCard() {
        return ResponseEntity.ok(employeeService.getEmployeesWithoutCard());
    }

    @GetMapping("/{employeeId}/groups")
    public ResponseEntity<List<Group>> getEmployeeGroups(@PathVariable Long employeeId) {
        return ResponseEntity.ok(groupService.getEmployeeGroups(employeeId));
    }

    @GetMapping
    public ResponseEntity<?> getEmployees(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null && size != null) {
            return ResponseEntity.ok(employeeService.getAllActiveEmployees(page, size));
        }
        return ResponseEntity.ok(employeeService.getAllActiveEmployees());
    }


    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Employee not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFound(CompanyNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Company not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateEmployeeFound.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmployee(DuplicateEmployeeFound ex) {
        ErrorResponse error = new ErrorResponse("Another employee found", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    public record EmployeeCount(
            Long count
    ) {}

}
