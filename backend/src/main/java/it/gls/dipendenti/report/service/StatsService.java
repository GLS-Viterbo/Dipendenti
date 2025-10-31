package it.gls.dipendenti.report.service;

import it.gls.dipendenti.absence.model.Absence;
import it.gls.dipendenti.absence.model.AbsenceStatus;
import it.gls.dipendenti.absence.repository.AbsenceRepository;
import it.gls.dipendenti.access.service.AccessService;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.shift.repository.ShiftAssignmentRepository;
import it.gls.dipendenti.report.model.MonthlyWorkingHoursStatsDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class StatsService {

    private final AccessService accessService;
    private final AbsenceRepository absenceRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;

    public StatsService(AccessService accessService,
                        AbsenceRepository absenceRepository,
                        ShiftAssignmentRepository assignmentRepository,
                        EmployeeRepository employeeRepository) {
        this.accessService = accessService;
        this.absenceRepository = absenceRepository;
        this.assignmentRepository = assignmentRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Get monthly working hours statistics for an employee
     * @param employeeId employee id
     * @param yearMonth year and month (null = current month)
     * @return statistics DTO
     */
    public MonthlyWorkingHoursStatsDTO getMonthlyStats(Long employeeId, YearMonth yearMonth) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }

        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        if (yearMonth == null) {
            yearMonth = YearMonth.now();
        }

        // Hours worked this month
        int hoursWorked = accessService.calculateMonthlyWorkingHours(employeeId, yearMonth);

        // Absences this year
        LocalDate yearStart = LocalDate.of(yearMonth.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(yearMonth.getYear(), 12, 31);

        List<Absence> yearAbsences = absenceRepository
                .findByEmployeeIdAndDateRange(employeeId, yearStart, yearEnd)
                .stream()
                .filter(a -> a.status() == AbsenceStatus.APPROVED)
                .toList();

        int absencesCount = yearAbsences.size();

        // Attendance rate (percentage of days worked vs total working days)
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();

        // Don't count future days
        LocalDate effectiveEnd = monthEnd.isBefore(today) ? monthEnd : today;

        int totalWorkingDays = assignmentRepository
                .countByEmployeeIdAndDateRange(employeeId, monthStart, effectiveEnd);

        List<Absence> monthAbsences = absenceRepository
                .findByEmployeeIdAndDateRange(employeeId, monthStart, effectiveEnd)
                .stream()
                .filter(a -> a.status() == AbsenceStatus.APPROVED)
                .toList();

        int absenceDays = monthAbsences.size();
        int workedDays = totalWorkingDays - absenceDays;

        double attendanceRate = totalWorkingDays > 0
                ? (double) workedDays / totalWorkingDays * 100
                : 0.0;

        return new MonthlyWorkingHoursStatsDTO(
                hoursWorked,
                absencesCount,
                Math.round(attendanceRate * 100.0) / 100.0 // Round to 2 decimals
        );
    }
}