package it.gls.dipendenti.hr.service;

import it.gls.dipendenti.absence.model.EmployeeLeaveAccrual;
import it.gls.dipendenti.absence.service.AbsenceService;
import it.gls.dipendenti.hr.exception.ContractNotFoundException;
import it.gls.dipendenti.hr.exception.DuplicateContractException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Contract;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.ContractRepository;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ContractService {

    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final AbsenceService absenceService;

    public ContractService(EmployeeRepository employeeRepository,
                           ContractRepository contractRepository,
                           AbsenceService absenceService) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.absenceService = absenceService;
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

        boolean valid = contract.startDate().isBefore(TimeZoneUtils.todayCompanyDate());
        // Adding contract

        return  contractRepository.save(new Contract(
                contract.id(),
                contract.employeeId(),
                contract.startDate(),
                contract.endDate(),
                contract.monthlyWorkingHours(),
                valid
        ));
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

        // Invalido il contratto se è stata cambiata la data di fine ed è una data già passata
        if ((existingContract.endDate() != contract.endDate()) && contract.endDate().isBefore(TimeZoneUtils.nowCompanyTime().toLocalDate()) )
            contract = new Contract(
                    contract.id(),
                    contract.employeeId(),
                    contract.startDate(),
                    contract.endDate(),
                    contract.monthlyWorkingHours(),
                    false);
        // Se è cambiata la data di fine ed è una data futura ri-valido il contratto
        if ((existingContract.endDate() != contract.endDate()) && contract.endDate().isAfter(TimeZoneUtils.nowCompanyTime().toLocalDate()) )
            contract = new Contract(
                    contract.id(),
                    contract.employeeId(),
                    contract.startDate(),
                    contract.endDate(),
                    contract.monthlyWorkingHours(),
                    true);

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

    public List<Contract> getEmployeeContracts(Long employeeId) {
        return contractRepository.getAllEmployeeContracts(employeeId);
    }

    private void validateContractData(Contract contract) {
        if (contract.endDate() != null && contract.endDate().isBefore(contract.startDate()))
            throw new IllegalArgumentException("End date cannot be before start date");
        if (contract.monthlyWorkingHours() < 0)
            throw new IllegalArgumentException("Monthly working hours must be positive");
    }


}
