package it.gls.dipendenti.absence.service;

import it.gls.dipendenti.absence.exception.AbsenceNotFoundException;
import it.gls.dipendenti.absence.exception.BalanceNotFoundException;
import it.gls.dipendenti.absence.exception.OverlappingAbsenceException;
import it.gls.dipendenti.absence.model.*;
import it.gls.dipendenti.absence.repository.*;
import it.gls.dipendenti.hr.exception.ContractNotFoundException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Contract;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.ContractRepository;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final EmployeeLeaveAccrualRepository accrualRepository;
    private final EmployeeLeaveBalanceRepository balanceRepository;
    private final HolidayRepository holidayRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;

    public AbsenceService(AbsenceRepository absenceRepository,
                          EmployeeLeaveAccrualRepository accrualRepository,
                          EmployeeLeaveBalanceRepository balanceRepository,
                          HolidayRepository holidayRepository,
                          EmployeeRepository employeeRepository,
                          ContractRepository contractRepository) {
        this.absenceRepository = absenceRepository;
        this.accrualRepository = accrualRepository;
        this.balanceRepository = balanceRepository;
        this.holidayRepository = holidayRepository;
        this.employeeRepository = employeeRepository;
        this.contractRepository = contractRepository;
    }

    /**
     * Initializing employee accrual and balance
     * @param accrual monthly accrual of employee
     */
    @Transactional
    public void initializeEmployeeLeave(EmployeeLeaveAccrual accrual) {
        if (employeeRepository.findById(accrual.employeeId()).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        accrualRepository.save(accrual);

        // Calculate past gained permits
        BigDecimal vacationAccrued = BigDecimal.ZERO;
        BigDecimal rolAccrued = BigDecimal.ZERO;

        Contract employeeContract = contractRepository.getByEmployeeId(accrual.employeeId()).orElseThrow(ContractNotFoundException::new);
        LocalDate contractStart = employeeContract.startDate();
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        // Check
        if (contractStart.getYear() > currentYear) {
            return;
        }
        // Finding when to start to update balance
        LocalDate startOfAccrual;
        if (contractStart.getYear() < currentYear) {
            startOfAccrual = LocalDate.of(currentYear, 1, 1);
        } else {
            if (contractStart.getDayOfMonth() <= 15) {
                startOfAccrual = contractStart.withDayOfMonth(1);
            } else {
                startOfAccrual = contractStart.plusMonths(1).withDayOfMonth(1);
            }
        }

        LocalDate monthCursor = startOfAccrual;
        LocalDate endOfYear = LocalDate.of(currentYear, 12, 31);

        BigDecimal vacationDaysPerMonth = accrual.vacationHoursPerMonth();
        BigDecimal rolHoursPerMonth = accrual.rolHoursPerMonth();

        while (!monthCursor.isAfter(now) && !monthCursor.isAfter(endOfYear)) {
            vacationAccrued = vacationAccrued.add(vacationDaysPerMonth);
            rolAccrued = rolAccrued.add(rolHoursPerMonth);
            monthCursor = monthCursor.plusMonths(1);
        }

        balanceRepository.save(new EmployeeLeaveBalance(
                null, accrual.employeeId(), vacationAccrued, rolAccrued
        ));


    }

    /**
     * Monthly job to update employee permits balance
     */
    @Transactional
    public void monthlyAccrualJob() {
        // TODO add cron job every month
        List<EmployeeLeaveAccrual> accruals = accrualRepository.findAll();

        for (EmployeeLeaveAccrual accrual : accruals) {
            // Verifying that employee has valid contract
            Optional<Contract> contract = contractRepository.getByEmployeeId(accrual.employeeId());
            if (contract.isPresent() && contract.get().valid()) {
                balanceRepository.addVacationHours(accrual.employeeId(), accrual.vacationHoursPerMonth());
                balanceRepository.addRolHours(accrual.employeeId(), accrual.rolHoursPerMonth());
            }
        }
    }

    /**
     * Used from administration to create an already approved permit request
     * @param absence absence to create
     * @return absence with new id
     */
    @Transactional
    public Absence createApprovedAbsence(Absence absence) {
        validateAbsence(absence);

        Absence approvedAbsence = new Absence(
                null,
                absence.employeeId(),
                absence.type(),
                absence.startDate(),
                absence.endDate(),
                absence.startTime(),
                absence.endTime(),
                absence.hoursCount(),
                AbsenceStatus.APPROVED,
                absence.note(),
                ZonedDateTime.now(),
                false
        );

        // TODO: update dayCount e hoursCount using module for work shifts
        // TODO: Verificare giorni festivi

        Absence saved = absenceRepository.save(approvedAbsence);

        // Aggiorna il saldo solo per VACATION e ROL
        updateBalanceForAbsence(saved, false);

        return saved;
    }

    /**
     * Deletes absence and restores balance
     * @param absenceId absence id
     */
    @Transactional
    public void deleteAbsence(Long absenceId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(AbsenceNotFoundException::new);

        updateBalanceForAbsence(absence, true);
        absenceRepository.delete(absenceId);
    }

    /**
     * Today's approved absences
     * @return today's absences
     */
    public Long getTodayCount() {
        return absenceRepository.getTodayCount();
    }

    /**
     * Future absences to approve
     * @return absences to approve
     */
    public Long getFutureToApprove() {
        return absenceRepository.getToApproveCount();
    }

    /**
     * Rol and vacations in a period of time
     * @param employeeId employee id
     * @param startDate start date
     * @param endDate end date
     */
    public void getUsedLeaveInPeriod(Long employeeId, LocalDate startDate, LocalDate endDate) {
        // TODO Ritornare il conto delle ore di ferie e di permessi usati nell'intervallo basandosi sui turni
    }

    /**
     * Returns id of employees with negative balance
     * @return id of employees
     */
    public List<EmployeeLeaveBalance> getNegativeBalances() {
        return balanceRepository.findEmployeesWithNegativeBalance();
    }

    /**
     * Returns the balance of an employee
     * @param employeeId id of employee
     * @return balance of employee
     */
    public EmployeeLeaveBalance getEmployeeBalance(Long employeeId) {
        return balanceRepository.findByEmployeeId(employeeId)
                .orElseThrow(BalanceNotFoundException::new);
    }

    /**
     * Returns the accrual rates of an employee
     * @param employeeId id of employee
     * @return accrual of employee
     */
    public EmployeeLeaveAccrual getEmployeeAccrual(Long employeeId) {
        return accrualRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BalanceNotFoundException("Accrual not found"));
    }

    /**
     * Updates employee accrual
     * The new accrual rate will start from next month
     * @param newAccrual new accrual for employee
     */
    @Transactional
    public void updateAccrualRate(Long employeeId, EmployeeLeaveAccrual newAccrual) {
        if (employeeId == null)
            throw new IllegalArgumentException("Employee id is null");
        if(employeeRepository.findById(employeeId).isEmpty())
            throw new EmployeeNotFoundException();

        EmployeeLeaveAccrual accrual = accrualRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BalanceNotFoundException("Accrual not found for this employee"));

        accrualRepository.update(new EmployeeLeaveAccrual(
                accrual.id(),
                employeeId,
                newAccrual.vacationHoursPerMonth(),
                newAccrual.rolHoursPerMonth()
        ));
    }

    /**
     * Updates employee balance
     * @param newBalance new balance for employee
     */
    @Transactional
    public void updateBalance(Long employeeId, EmployeeLeaveBalance newBalance) {
        if (employeeId == null)
            throw new IllegalArgumentException("Employee id is null");
        if(employeeRepository.findById(employeeId).isEmpty())
            throw new EmployeeNotFoundException();

        EmployeeLeaveBalance balance = balanceRepository.findByEmployeeId(employeeId)
                .orElseThrow(BalanceNotFoundException::new);

        balanceRepository.update(new EmployeeLeaveBalance(
                balance.id(),
                employeeId,
                newBalance.vacationAvailable(),
                newBalance.rolAvailable()
        ));
    }

    /**
     * All absences of an employee
     * @param employeeId employee id
     * @return list of absences
     */
    public List<Absence> getEmployeeAbsences(Long employeeId) {
        return absenceRepository.findByEmployeeId(employeeId);
    }

    /**
     * Obtains all absences in a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of absences
     */
    public List<Absence> getAbsencesByDateRange(LocalDate startDate, LocalDate endDate) {
        return absenceRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Obtains all detailed absences in a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of absences
     */
    public List<DetailedAbsence> getDetailedAbsencesByDateRange(LocalDate startDate, LocalDate endDate) {
        List<Absence> absences = absenceRepository.findByDateRange(startDate, endDate);
        List<DetailedAbsence> output = new ArrayList<>();
        for (Absence abs : absences) {
            Optional<Employee> employee = employeeRepository.findById(abs.employeeId());
            output.add(new DetailedAbsence(
                    abs.id(),
                    abs.employeeId(),
                    employee.map(value -> "%s %s".formatted(value.name(), value.surname())).orElse("SCONOSCIUTO"),
                    abs.type().name(),
                    abs.startDate(),
                    abs.endDate(),
                    abs.startTime(),
                    abs.endTime(),
                    abs.hoursCount(),
                    abs.status().name(),
                    abs.note(),
                    abs.createdAt(),
                    abs.deleted()

            ))
        }

    }


    private void validateAbsence(Absence absence) {
        if (absence.employeeId() == null) {
            throw new IllegalArgumentException("Employee ID cannot be null");
        }

        // Verifies that employee exist
        if (employeeRepository.findById(absence.employeeId()).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        if (absence.startDate() == null || absence.endDate() == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }

        if (absence.endDate().isBefore(absence.startDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        // Overlapping
        if (absenceRepository.hasOverlappingAbsences(
                absence.employeeId(),
                absence.startDate(),
                absence.endDate(),
                absence.id())) {
            throw new OverlappingAbsenceException();
        }
    }

    private void updateBalanceForAbsence(Absence absence, boolean restore) {
        if (absence.type() == AbsenceType.PERMIT || absence.type() == AbsenceType.SICK_LEAVE) {
            // This does not change balance
            return;
        }

        // Amount of hours
        BigDecimal amount = BigDecimal.valueOf(absence.hoursCount());

        if (restore) {
            // Restore balance
            if (absence.type() == AbsenceType.VACATION) {
                balanceRepository.addVacationHours(
                        absence.employeeId(),
                        BigDecimal.valueOf(absence.hoursCount())
                );
            } else {
                balanceRepository.addRolHours(absence.employeeId(), amount);
            }
        } else {
            // Decrease balance
            if (absence.type() == AbsenceType.VACATION) {
                balanceRepository.subtractVacationHours(
                        absence.employeeId(),
                        BigDecimal.valueOf(absence.hoursCount())
                );
            } else {
                balanceRepository.subtractRolHours(absence.employeeId(), amount);
            }
        }
    }

    public record DetailedAbsence(
            Long id,
            Long employeeId,
            String employeeName,
            String type,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer hoursCount,
            String status,
            String note,
            Timestamp createdAt,
            boolean deleted

    ) {

    }
}