package it.gls.dipendenti.hr.service;

import it.gls.dipendenti.hr.exception.ContractNotFoundException;
import it.gls.dipendenti.hr.exception.DuplicateContractException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Contract;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.ContractRepository;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ContractService {

    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;

    public ContractService(EmployeeRepository employeeRepository, ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Contract setContract(Contract contract) {
        validateContractData(contract);
        if (contract.employeeId() == null)
            throw new IllegalArgumentException("Employee ID cannot be null");

        // Checking if employee exist
        Optional<Employee> employee = employeeRepository.findById(contract.employeeId());
        if(employee.isEmpty())
            throw new EmployeeNotFoundException();

        // Checking if employee has an active contract
        if (contractRepository.getByEmployeeId(contract.employeeId()).isPresent())
            throw new DuplicateContractException();

        // Adding contract
        return contractRepository.save(contract);
    }

    @Transactional
    public void updateContract(Contract contract) {

        if (contract.id() == null)
            throw new ContractNotFoundException();
        validateContractData(contract);

        // Checking if contract to modify is actually of the given employee id
        Contract existingContract = contractRepository.getById(contract.id())
                .orElseThrow(ContractNotFoundException::new);

        if (!existingContract.employeeId().equals(contract.employeeId()))
            throw new IllegalArgumentException("Cannot change contract's employee");

        if (!contractRepository.update(contract))
            throw new ContractNotFoundException();
    }
    @Transactional
    public void invalidateContract(Long contractId) {
        if (contractId == null)
            throw new ContractNotFoundException();
        boolean invalidated = contractRepository.invalidate(contractId);
        if(!invalidated)
            throw new ContractNotFoundException();

    }
    @Transactional
    public Contract getContractById(Long id) {
        if (id == null)
            throw new IllegalArgumentException("Contract id cannot be null");
        Optional<Contract> contract = contractRepository.getById(id);
        return contract.orElseThrow(ContractNotFoundException::new);
    }

    @Transactional
    public void invalidateEmployeeContract(Long employeeId) {
        if (employeeId == null)
            throw new IllegalArgumentException("Employee ID cannot be null");
        Optional<Contract> contract = contractRepository.getByEmployeeId(employeeId);
        if(contract.isEmpty())
            throw new IllegalArgumentException("Employee doesn't have a contract");
        invalidateContract(contract.get().id());

    }

    public Optional<Contract> getEmployeeContract(Long employeeId) {
        return contractRepository.getByEmployeeId(employeeId);
    }

    private void validateContractData(Contract contract) {
        if (contract.endDate() != null && contract.endDate().isBefore(contract.startDate()))
            throw new IllegalArgumentException("End date cannot be before start date");
        if (contract.monthlyWorkingHours() < 0)
            throw new IllegalArgumentException("Monthly working hours must be positive");
    }


}
