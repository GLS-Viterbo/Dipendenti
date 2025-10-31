package it.gls.dipendenti.hr.controller;

import it.gls.dipendenti.hr.exception.CompanyNotFoundException;
import it.gls.dipendenti.hr.exception.DuplicateCompanyFoundException;
import it.gls.dipendenti.hr.model.Company;
import it.gls.dipendenti.hr.service.CompanyService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public ResponseEntity<Company> createCompany(@RequestBody Company company) {
        Company createdCompany = companyService.createCompany(company);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable Long id) {
        Company company = companyService.getCompanyById(id);
        return ResponseEntity.ok(company);
    }

    @GetMapping
    public ResponseEntity<List<Company>> getAllCompanies(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(companyService.getAllActiveCompanies());
        }
        return ResponseEntity.ok(companyService.getAllCompanies());
    }


    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        companyService.updateCompany(new Company(
                id,
                company.name(),
                company.active()
        ));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCompany(@PathVariable Long id) {
        companyService.deactivateCompany(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateCompany(@PathVariable Long id) {
        companyService.activateCompany(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long id) {
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/employees/count")
    public ResponseEntity<EmployeeCountResponse> getEmployeeCount(@PathVariable Long id) {
        Long count = companyService.getEmployeeCount(id);
        return ResponseEntity.ok(new EmployeeCountResponse(count));
    }

    @GetMapping("/count")
    public ResponseEntity<CompanyCountResponse> getTotalCount() {
        Long count = companyService.getTotalCount();
        return ResponseEntity.ok(new CompanyCountResponse(count));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFound(CompanyNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("Company not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateCompanyFoundException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCompany(DuplicateCompanyFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    public record EmployeeCountResponse(Long count) {}
    public record CompanyCountResponse(Long count) {}
}