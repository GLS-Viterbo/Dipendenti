package it.gls.dipendenti.hr.service;

import it.gls.dipendenti.hr.exception.CompanyNotFoundException;
import it.gls.dipendenti.hr.exception.DuplicateCompanyFoundException;
import it.gls.dipendenti.hr.model.Company;
import it.gls.dipendenti.hr.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Creates a new company
     * Cannot exist two companies with same name
     * @param company company fields
     * @return company with generated id
     */
    @Transactional
    public Company createCompany(Company company) {
        if (company.name() == null || company.name().isBlank())
            throw new IllegalArgumentException("Company name cannot be null or empty");

        if (companyRepository.findByName(company.name()).isPresent())
            throw new DuplicateCompanyFoundException("Company with name %s already exists"
                    .formatted(company.name()));

        return companyRepository.save(company);
    }

    /**
     * Returns a company with given id
     * @param id id of the company
     * @return company
     */
    public Company getCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(CompanyNotFoundException::new);
    }

    /**
     * Returns all companies
     * @return List of companies
     */
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    /**
     * Returns all active companies
     * @return List of active companies
     */
    public List<Company> getAllActiveCompanies() {
        return companyRepository.findAllActive();
    }

    /**
     * Returns a company by name
     * @param name name of the company
     * @return company
     */
    public Company getCompanyByName(String name) {
        return companyRepository.findByName(name)
                .orElseThrow(CompanyNotFoundException::new);
    }

    /**
     * Modifies a company attributes
     * @param company the company to modify
     */
    @Transactional
    public void updateCompany(Company company) {
        if (company.id() == null)
            throw new IllegalArgumentException("Company ID cannot be null");

        // Check if trying to change name to one already in use
        if (company.name() != null) {
            companyRepository.findByName(company.name())
                    .ifPresent(existing -> {
                        if (!existing.id().equals(company.id()))
                            throw new DuplicateCompanyFoundException("Company name already in use");
                    });
        }

        if (!companyRepository.update(company))
            throw new CompanyNotFoundException();
    }

    /**
     * Deactivates a company
     * @param companyId id of the company to deactivate
     */
    @Transactional
    public void deactivateCompany(Long companyId) {
        if (companyId == null)
            throw new IllegalArgumentException("Company ID cannot be null");

        if (!companyRepository.deactivate(companyId))
            throw new CompanyNotFoundException();
    }

    /**
     * Activates a company
     * @param companyId id of the company to activate
     */
    @Transactional
    public void activateCompany(Long companyId) {
        if (companyId == null)
            throw new IllegalArgumentException("Company ID cannot be null");

        if (!companyRepository.activate(companyId))
            throw new CompanyNotFoundException();
    }

    /**
     * Deletes a company (only if no employees are associated)
     * @param companyId id of the company to delete
     */
    @Transactional
    public void deleteCompany(Long companyId) {
        if (companyId == null)
            throw new IllegalArgumentException("Company ID cannot be null");

        // Check if company has employees
        Long employeeCount = companyRepository.countEmployees(companyId);
        if (employeeCount > 0) {
            throw new IllegalStateException("Cannot delete company with %d associated employee(s)"
                    .formatted(employeeCount));
        }

        if (!companyRepository.delete(companyId))
            throw new CompanyNotFoundException();
    }

    /**
     * Returns the number of employees associated with a company
     * @param companyId id of the company
     * @return number of employees
     */
    public Long getEmployeeCount(Long companyId) {
        if (companyId == null)
            throw new IllegalArgumentException("Company ID cannot be null");

        return companyRepository.countEmployees(companyId);
    }

    /**
     * Returns the total number of companies
     * @return total count
     */
    public Long getTotalCount() {
        return companyRepository.count();
    }
}