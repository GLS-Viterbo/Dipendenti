package it.gls.dipendenti.hr.controller;

import it.gls.dipendenti.hr.exception.ContractNotFoundException;
import it.gls.dipendenti.hr.exception.DuplicateContractException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Contract;
import it.gls.dipendenti.hr.service.ContractService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    public ResponseEntity<Contract> createContract(@RequestBody Contract contract) {
        Contract createdContract = contractService.setContract(contract);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdContract);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateContract(@PathVariable Long id, @RequestBody Contract contract) {
        // Adding path id to contract object
        Contract contractWithId = new Contract(
                id,
                contract.employeeId(),
                contract.startDate(),
                contract.endDate(),
                contract.monthlyWorkingHours(),
                contract.valid()
        );
        contractService.updateContract(contractWithId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> invalidateContract(@PathVariable Long id) {
        contractService.invalidateContract(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/employee/{employeeId}")
    public ResponseEntity<Void> invalidateEmployeeContract(@PathVariable Long employeeId) {
        contractService.invalidateEmployeeContract(employeeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Contract> getEmployeeContract(@PathVariable Long employeeId) {
        Optional<Contract> contract = contractService.getEmployeeContract(employeeId);
        return contract.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/employee/{employeeId}/all")
    public ResponseEntity<List<Contract>> getEmployeeContracts(@PathVariable Long employeeId) {
        return ResponseEntity.ok(contractService.getEmployeeContracts(employeeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(@PathVariable Long id) {
        Contract contract = contractService.getContractById(id);
        return ResponseEntity.ok(contract);
    }

    // Exception Handlers
    @ExceptionHandler(ContractNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContractNotFound(ContractNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Contract not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateContractException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateContract(DuplicateContractException ex) {
        ErrorResponse error = new ErrorResponse("Employee already has an active contract", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Employee not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

}
