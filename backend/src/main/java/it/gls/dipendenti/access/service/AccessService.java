package it.gls.dipendenti.access.service;

import it.gls.dipendenti.access.dto.AccessAnomalyDTO;
import it.gls.dipendenti.access.exception.CardNotFoundException;
import it.gls.dipendenti.access.exception.LogNotFoundException;
import it.gls.dipendenti.access.model.AccessLog;
import it.gls.dipendenti.access.model.AccessType;
import it.gls.dipendenti.access.model.Card;
import it.gls.dipendenti.access.model.CardAssignment;
import it.gls.dipendenti.access.repository.AccessRepository;
import it.gls.dipendenti.access.repository.CardAssignmentRepository;
import it.gls.dipendenti.access.repository.CardRepository;
import it.gls.dipendenti.auth.model.CustomUserDetails;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.util.Page;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.document.AbstractXlsView;

import javax.security.auth.login.LoginException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccessService {

    private final AccessRepository accessRepository;
    private final CardService cardService;
    private final CardRepository cardRepository;
    private final CardAssignmentService assignmentService;
    private final EmployeeRepository employeeRepository;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AccessService(AccessRepository accessRepository,
                         CardService cardService,
                         CardAssignmentService assignmentService,
                         EmployeeRepository employeeRepository,
                         CardRepository cardRepository) {
        this.accessRepository = accessRepository;
        this.cardService = cardService;
        this.assignmentService = assignmentService;
        this.employeeRepository = employeeRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public DetailedAccessLog readCard(String cardUid) {
        if (cardUid == null || cardUid.isBlank()) {
            throw new IllegalArgumentException("Invalid card UID");
        }

        Card card = cardService.getByUid(cardUid);
        if (card == null) {
            cardService.createCard(new Card(null, cardUid, false));
            return null;
        }
        if (card.deleted()) {
            throw new IllegalArgumentException("Card is deleted");
        }

        CardAssignment assignment = assignmentService.getCardAssignment(card.id());
        if (assignment == null) return null;

        Employee emp = employeeRepository.findById(assignment.employeeId()).orElse(null);

        // ✅ Usa TimeZoneUtils per coerenza
        Instant now = TimeZoneUtils.nowInstant();
        AccessType newType = accessRepository.getNextType(assignment.employeeId(), now);

        AccessLog log = accessRepository.addLog(
                new AccessLog(null, assignment.employeeId(), card.id(),
                        now, newType, false, null, false)
        );


        return new DetailedAccessLog(
                log.id(),
                log.employeeId(),
                emp != null ? emp.name() : "SCONOSCIUTO",
                emp != null ? emp.surname() : "",
                card.id(),
                card.uid(),
                log.timestamp(),
                log.type().name(),
                log.modified(),
                log.deleted()
        );

    }

    @Transactional
    public AccessLog addManualLog(AccessLog log) {
        Employee employee = employeeRepository.findById(log.employeeId()).orElse(null);
        if (employee == null)
            throw new EmployeeNotFoundException();
        Card card = cardRepository.getCardById(
                assignmentService.getEmployeeAssignments(employee.id()).getFirst().cardId()).orElseThrow(CardNotFoundException::new);
        return accessRepository.addLog(
                new AccessLog(
                        null,
                        employee.id(),
                        card.id(),
                        log.timestamp(),
                        log.type(),
                        true,
                        Instant.now(),
                        false
                )
        );
    }

    @Transactional
    public void modifyLog(AccessLog accessLog) {
        if (accessLog.id() == null)
            throw new IllegalArgumentException("Log id is null");
        AccessLog oldLog = accessRepository.getById(accessLog.id()).orElseThrow(LogNotFoundException::new);
        if(!accessLog.employeeId().equals(oldLog.employeeId()))
            throw new IllegalArgumentException("Cannot change employee id");

        if(!accessRepository.modifyLog(
                new AccessLog(
                        accessLog.id(),
                        accessLog.employeeId(),
                        accessLog.cardId(),
                        accessLog.timestamp(),
                        accessLog.type(),
                        true,
                        Instant.now(),
                        accessLog.deleted()
                )
        )) {
            throw new LogNotFoundException();
        }

    }

    /**
     * Number of employees at work
     * @return number of employees at work
     */
    public Long getEmployeesWorking() {
        return accessRepository.getEmployeesAtWork(getCurrentUserCompanyId());
    }

    @Transactional
    public void deleteLog(Long logId) {
        if (logId == null)
            throw new IllegalArgumentException("Log id is null");
        if(!accessRepository.deleteLog(logId))
            throw new LogNotFoundException();
    }

    public List<AccessLog> getLogsInTimeRange(Instant startTime, Instant endTime) {
        if (endTime.isBefore(startTime))
            throw new IllegalArgumentException("Start time cannot be after end time");
        return accessRepository.getLogsInTimeRange(startTime, endTime, getCurrentUserCompanyId());
    }

    public List<AccessLog> getLogsInTimeRangeByEmployee(Long employeeId, Instant startTime, Instant endTime) {
        if (endTime.isBefore(startTime))
            throw new IllegalArgumentException("Start time cannot be after end time");
        if (employeeId == null)
            throw new IllegalArgumentException("Employee id is null");
        return accessRepository.getLogsInTimeRangeByEmployee(employeeId, startTime, endTime);
    }

    public List<AccessLog> getLogsForDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date is null");
        return accessRepository.getLogsByDate(date, getCurrentUserCompanyId());
    }

    public List<DetailedAccessLog> getDetailedLogsForDate(LocalDate date) {
        List<AccessLog> logs = accessRepository.getLogsByDate(date, getCurrentUserCompanyId());
        List<DetailedAccessLog> detailedLogs = new ArrayList<>();
        for (AccessLog log : logs) {
            Employee emp = employeeRepository.findById(log.employeeId()).orElse(null);
            Card card = cardRepository.getCardById(log.cardId()).orElse(null);
            detailedLogs.add(
                    new DetailedAccessLog(
                            log.id(),
                            log.employeeId(),
                            emp != null ? emp.name() : "SCONOSCIUTO",
                            emp != null ? emp.surname() : "",
                            card != null ? card.id() : null,
                            card != null ? card.uid() : "SCONOSCIUTO",
                            log.timestamp(),
                            log.type().name(),
                            log.modified(),
                            log.deleted()
                    )
            );
        }
        return detailedLogs;
    }

    public List<AccessLog> getEmployeeLogsForDate(Long employeeId, LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date is null");
        if (employeeId == null) throw new IllegalArgumentException("Employee id is null");
        return accessRepository.getLogsByEmployeeAndDate(employeeId, date);
    }

    public List<LocalDate> getWorkingDaysInRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null)
            throw new IllegalArgumentException("Start date is null");
        if (endDate == null)
            throw new IllegalArgumentException("End date is null");
        if (employeeId == null)
            throw new IllegalArgumentException("Employee id is null");
        return accessRepository.getDistinctLogDates(employeeId, startDate, endDate);
    }

    /**
     * Calculate working hours for an employee in a specific month
     * @param employeeId employee id
     * @param yearMonth year and month (e.g., YearMonth.of(2025, 10))
     * @return total working hours
     */
    public int calculateMonthlyWorkingHours(Long employeeId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<AccessLog> logs = accessRepository.getLogsInTimeRangeByEmployee(
                employeeId,
                TimeZoneUtils.startOfDay(startDate),
                TimeZoneUtils.endOfDay(endDate)
        );

        // Group logs by date
        Map<LocalDate, List<AccessLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(log ->
                        TimeZoneUtils.toCompanyDate(log.timestamp())
                ));

        int totalHours = 0;

        for (Map.Entry<LocalDate, List<AccessLog>> entry : logsByDate.entrySet()) {
            List<AccessLog> dayLogs = entry.getValue().stream()
                    .sorted(Comparator.comparing(AccessLog::timestamp))
                    .toList();

            // Calculate hours for pairs of IN/OUT
            for (int i = 0; i < dayLogs.size() - 1; i += 2) {
                if (dayLogs.get(i).type() == AccessType.IN &&
                        dayLogs.get(i + 1).type() == AccessType.OUT) {
                    long hours = ChronoUnit.HOURS.between(
                            dayLogs.get(i).timestamp(),
                            dayLogs.get(i + 1).timestamp()
                    );
                    totalHours += hours;
                }
            }
        }

        return totalHours;
    }


    // ======== ANOMALIES ===========

    // TODO Trovare dipendenti assenti senza un assenza o al lavoro con un assenza
    /**
     * Find anomalies for all employees in a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of anomalies grouped by employee
     */
    public List<AccessAnomalyDTO> findAllAnomalies(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date cannot be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        LocalDate today = LocalDate.now(TimeZoneUtils.COMPANY_ZONE);

        if (!endDate.isBefore(today)) {
            endDate = today.minusDays(1);
        }

        if (startDate.isAfter(endDate)) {
            return new ArrayList<>();
        }

        Instant startInstant = startDate.atStartOfDay(TimeZoneUtils.COMPANY_ZONE).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(TimeZoneUtils.COMPANY_ZONE).toInstant().minusNanos(1);

        var logs = accessRepository.getLogsInTimeRange(startInstant, endInstant, getCurrentUserCompanyId());

        // Raggruppa per dipendente e data
        var anomalies = new ArrayList<AccessAnomalyDTO>();
        var groupedByEmployeeAndDate = groupLogsByEmployeeAndDate(logs);

        groupedByEmployeeAndDate.forEach((key, logsForDay) -> {
            Long empId = key.employeeId();
            LocalDate date = key.date();
            anomalies.addAll(detectAnomaliesForDay(empId, date, logsForDay));
        });

        return anomalies;
    }



    /**
     * Helper to group logs by employee and date
     */
    private java.util.Map<EmployeeDateKey, List<AccessLog>> groupLogsByEmployeeAndDate(List<AccessLog> logs) {
        var grouped = new java.util.HashMap<EmployeeDateKey, List<AccessLog>>();

        for (AccessLog log : logs) {
            LocalDate date = log.timestamp().atZone(TimeZoneUtils.COMPANY_ZONE).toLocalDate();
            EmployeeDateKey key = new EmployeeDateKey(log.employeeId(), date);

            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(log);
        }

        return grouped;
    }

    private record EmployeeDateKey(Long employeeId, LocalDate date) {}

    /**
     * Detect anomalies for a specific day
     */
    private List<AccessAnomalyDTO> detectAnomaliesForDay(Long employeeId, LocalDate date, List<AccessLog> logs) {
        List<AccessAnomalyDTO> anomalies = new ArrayList<>();
        Employee employee = employeeRepository.findById(employeeId).orElse(null);

        // Se il dipendente è stato eliminato non ritorno l'anomalia
        if (employee != null && employee.deleted())
            return List.of();

        String employeeName = employee != null ? "%s %s".formatted(employee.name(), employee.surname()) : "SCONOSCIUTO";

        if (logs.isEmpty()) {
            return anomalies;
        }

        if (logs.size() % 2 != 0) {
            AccessLog lastLog = logs.stream()
                    .max(Comparator.comparing(AccessLog::timestamp))
                    .orElse(null);

            if (lastLog.type() == AccessType.IN) {
                anomalies.add(new AccessAnomalyDTO(
                        employeeId,
                        date,
                        AccessAnomalyDTO.AnomalyType.MISSING_EXIT,
                        "Manca la lettura in uscita",
                        employeeName
                ));
            } else {
                anomalies.add(new AccessAnomalyDTO(
                        employeeId,
                        date,
                        AccessAnomalyDTO.AnomalyType.ODD_NUMBER_LOGS,
                        "Numero di letture dispari",
                        employeeName
                ));
            }
        }

        for (int i = 0; i < logs.size() - 1; i++) {
            AccessLog current = logs.get(i);
            AccessLog next = logs.get(i + 1);

            if (current.type() == next.type()) {
                // Converti in ora locale Roma per il messaggio
                var currentTime = current.timestamp().atZone(TimeZoneUtils.COMPANY_ZONE).toLocalTime();
                var nextTime = next.timestamp().atZone(TimeZoneUtils.COMPANY_ZONE).toLocalTime();

                anomalies.add(new AccessAnomalyDTO(
                        employeeId,
                        date,
                        current.type() == AccessType.IN ?
                                AccessAnomalyDTO.AnomalyType.MISSING_EXIT :
                                AccessAnomalyDTO.AnomalyType.MISSING_ENTRY,
                        String.format("Letture uguali consecutive alle %s e alle %s", currentTime, nextTime),
                        employeeName
                ));
            }
        }

        return anomalies;
    }

    /**
     * Check if employee has any anomalies in a date range
     * @param employeeId employee id
     * @param startDate start date
     * @param endDate end date
     * @return true if anomalies exist
     */
    public boolean hasAnomaliesInRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        if (employeeId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        LocalDate today = LocalDate.now(TimeZoneUtils.COMPANY_ZONE);
        if (!endDate.isBefore(today)) {
            endDate = today.minusDays(1);
        }

        if (startDate.isAfter(endDate)) {
            return false;
        }

        Instant startInstant = TimeZoneUtils.startOfDay(startDate);
        Instant endInstant = TimeZoneUtils.endOfDay(endDate);

        List<AccessLog> logs = accessRepository.getLogsInTimeRangeByEmployee(
                employeeId, startInstant, endInstant
        );

        // Group by date
        Map<LocalDate, List<AccessLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(log ->
                        TimeZoneUtils.toCompanyDate(log.timestamp())
                ));

        // Check each day for anomalies
        for (List<AccessLog> dayLogs : logsByDate.values()) {
            if (hasAnomaliesForDay(dayLogs)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper to detect anomalies in a single day's logs
     */
    private boolean hasAnomaliesForDay(List<AccessLog> logs) {
        if (logs.isEmpty()) {
            return false;
        }

        // Sort by timestamp
        logs = logs.stream()
                .sorted(Comparator.comparing(AccessLog::timestamp))
                .toList();

        // Odd number of logs
        if (logs.size() % 2 != 0) {
            return true;
        }

        // Check for consecutive same types
        for (int i = 0; i < logs.size() - 1; i++) {
            if (logs.get(i).type() == logs.get(i + 1).type()) {
                return true;
            }
        }

        return false;
    }

    public boolean isAtWork(Long employeeId) {
        AccessLog log = accessRepository.getLastLogByEmployee(employeeId).orElse(null);
        if (log != null) {
            return log.type() == AccessType.IN;
        }
        return false;
    }

    private Long getCurrentUserCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getCompanyId();
    }

    public record DetailedAccessLog(
            Long id,
            Long employeeId,
            String employeeName,
            String employeeSurname,
            Long cardId,
            String cardUid,
            Instant timestamp,
            String type,
            boolean modified,
            boolean deleted
    ) {}
}
