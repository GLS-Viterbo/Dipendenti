package it.gls.dipendenti.report.service;

import it.gls.dipendenti.absence.model.Absence;
import it.gls.dipendenti.absence.model.AbsenceStatus;
import it.gls.dipendenti.absence.model.AbsenceType;
import it.gls.dipendenti.absence.model.EmployeeLeaveBalance;
import it.gls.dipendenti.absence.repository.AbsenceRepository;
import it.gls.dipendenti.absence.repository.EmployeeLeaveBalanceRepository;
import it.gls.dipendenti.absence.repository.HolidayRepository;
import it.gls.dipendenti.access.model.AccessLog;
import it.gls.dipendenti.access.repository.AccessRepository;
import it.gls.dipendenti.access.service.AccessService;
import it.gls.dipendenti.auth.model.CustomUserDetails;
import it.gls.dipendenti.hr.model.Contract;
import it.gls.dipendenti.util.ExcelStyles;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.repository.ContractRepository;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.shift.model.ShiftAssignment;
import it.gls.dipendenti.shift.repository.ShiftAssignmentRepository;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MonthlyReportService {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyReportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmployeeRepository employeeRepository;
    private final AccessRepository accessRepository;
    private final AccessService accessService;
    private final AbsenceRepository absenceRepository;
    private final EmployeeLeaveBalanceRepository balanceRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ContractRepository contractRepository;
    private final HolidayRepository holidayRepository;

    public MonthlyReportService(
            EmployeeRepository employeeRepository,
            AccessRepository accessRepository,
            AccessService accessService,
            AbsenceRepository absenceRepository,
            EmployeeLeaveBalanceRepository balanceRepository,
            ShiftAssignmentRepository shiftAssignmentRepository,
            ContractRepository contractRepository,
            HolidayRepository holidayRepository) {
        this.employeeRepository = employeeRepository;
        this.accessRepository = accessRepository;
        this.accessService = accessService;
        this.absenceRepository = absenceRepository;
        this.balanceRepository = balanceRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.contractRepository = contractRepository;
        this.holidayRepository = holidayRepository;
    }

    /**
     * Genera il report mensile in formato Excel
     */
    public byte[] generateMonthlyReport(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        logger.info("Generazione report per il periodo: {} - {}", startDate, endDate);

        Workbook workbook = new XSSFWorkbook();
        ExcelStyles styles = new ExcelStyles();

        try {
            // Sheet 1: Riepilogo Generale
            createSummarySheet(workbook, styles, startDate, endDate);

            // Sheet 2: Dettaglio Giornaliero
            // createDailyDetailSheet(workbook, styles, startDate, endDate);

            // Sheet 3: Assenze
            createAbsencesSheet(workbook, styles, startDate, endDate);

            // Sheet 4: Anomalie
            createAnomaliesSheet(workbook, styles, startDate, endDate);

            // Sheet 5: Saldi Ferie/ROL
            createBalanceSheet(workbook, styles, startDate, endDate);

            // Converti in byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            logger.info("Report generato con successo");
            return outputStream.toByteArray();

        } catch (Exception e) {
            logger.error("Errore durante la generazione del report", e);
            throw new RuntimeException("Impossibile generare il report", e);
        }
    }

    // ============= SHEET 1: RIEPILOGO GENERALE =============

    private void createSummarySheet(Workbook workbook, ExcelStyles styles, LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Riepilogo");

        // Larghezza colonne
        sheet.setColumnWidth(0, 30 * 256); // Nome dipendente
        for (int i = 1; i < 12; i++) {
            sheet.setColumnWidth(i, 15 * 256);
        }

        // Header del report
        createReportHeader(workbook, sheet, styles, startDate, endDate);

        // Header della tabella
        int currentRow = 2;
        createSummaryTableHeader(sheet, workbook, styles, currentRow);

        // Dati dipendenti
        currentRow = 3;
        List<Employee> employees = employeeRepository.findAll(getCurrentUserCompanyId());

        for (Employee employee : employees) {
            createEmployeeSummaryRow(workbook, sheet, styles, currentRow++, employee, startDate, endDate);
        }

        logger.info("Sheet 'Riepilogo' creato con {} dipendenti", employees.size());
    }

    private void createReportHeader(Workbook workbook, Sheet sheet, ExcelStyles styles,
                                    LocalDate startDate, LocalDate endDate) {
        Row headerRow = sheet.createRow(0);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue(String.format("Report Dipendenti dal %s al %s",
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER)));

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 11));
        headerCell.setCellStyle(styles.getBoldStyle(workbook));
    }

    private void createSummaryTableHeader(Sheet sheet, Workbook workbook, ExcelStyles styles, int rowIndex) {
        Row header = sheet.createRow(rowIndex);
        String[] headers = {
                "Dipendente",
                "Giorni Lavorati",
                "Ore Lavorate",
                "Ore Previste",
                "Delta Ore",
                "Straordinari",
                "Ore Ferie",
                "Ore ROL",
                "Giorni Malattia",
                "Giorni Permesso",
                "Ferie Residue",
                "ROL Residui"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.getBoldStyle(workbook));
        }
    }

    private void createEmployeeSummaryRow(Workbook workbook, Sheet sheet, ExcelStyles styles,
                                          int rowIndex, Employee employee,
                                          LocalDate startDate, LocalDate endDate) {
        Row row = sheet.createRow(rowIndex);
        CreationHelper factory = workbook.getCreationHelper();
        Drawing<?> drawing = sheet.createDrawingPatriarch();

        int col = 0;

        // Calcola le metriche
        EmployeeMetrics metrics = calculateEmployeeMetrics(employee.id(), startDate, endDate);

        // Column 0: Nome Dipendente
        Cell nameCell = row.createCell(col++);
        nameCell.setCellValue(employee.surname() + " " + employee.name());
        nameCell.setCellStyle(styles.getBoldStyle(workbook));

        // Column 1: Giorni Lavorati (con tooltip)
        Cell workDaysCell = row.createCell(col++);
        workDaysCell.setCellStyle(styles.getCenterStyle(workbook));
        workDaysCell.setCellValue(metrics.workedDays().size());

        addTooltip(factory, drawing, workDaysCell, row,
                "Giorni lavorati:\n" + metrics.workedDays().stream()
                        .map(d -> d.format(DATE_FORMATTER))
                        .collect(Collectors.joining("\n")));

        // Column 2: Ore Lavorate
        Cell workedHoursCell = row.createCell(col++);
        workedHoursCell.setCellStyle(styles.getDecimalStyle(workbook));
        workedHoursCell.setCellValue(metrics.workedHours());

        // Column 3: Ore Previste
        Cell expectedHoursCell = row.createCell(col++);
        expectedHoursCell.setCellStyle(styles.getDecimalStyle(workbook));
        expectedHoursCell.setCellValue(metrics.expectedHours());

        // Column 4: Delta Ore
        Cell deltaCell = row.createCell(col++);
        double delta = metrics.workedHours() - metrics.expectedHours();
        deltaCell.setCellStyle(delta >= 0 ?
                styles.getPositiveStyle(workbook) :
                styles.getNegativeStyle(workbook));
        deltaCell.setCellValue(delta);

        // Column 5: Straordinari
        Cell overtimeCell = row.createCell(col++);
        overtimeCell.setCellStyle(styles.getDecimalStyle(workbook));
        overtimeCell.setCellValue(Math.max(0, delta));

        // Column 6: Ore Ferie (con tooltip)
        Cell vacationCell = row.createCell(col++);
        vacationCell.setCellStyle(styles.getDecimalStyle(workbook));
        vacationCell.setCellValue(metrics.vacationHours());

        addTooltip(factory, drawing, vacationCell, row,
                buildAbsenceTooltip("Ferie", metrics.vacationDetails()));

        // Column 7: Ore ROL (con tooltip)
        Cell rolCell = row.createCell(col++);
        rolCell.setCellStyle(styles.getDecimalStyle(workbook));
        rolCell.setCellValue(metrics.rolHours());

        addTooltip(factory, drawing, rolCell, row,
                buildAbsenceTooltip("ROL", metrics.rolDetails()));

        // Column 8: Giorni Malattia (con tooltip)
        Cell sickCell = row.createCell(col++);
        sickCell.setCellStyle(styles.getCenterStyle(workbook));
        sickCell.setCellValue(metrics.sickDays().size());

        addTooltip(factory, drawing, sickCell, row,
                "Giorni di malattia:\n" + metrics.sickDays().stream()
                        .map(d -> d.format(DATE_FORMATTER))
                        .collect(Collectors.joining("\n")));

        // Column 9: Giorni Permesso (con tooltip)
        Cell permitCell = row.createCell(col++);
        permitCell.setCellStyle(styles.getCenterStyle(workbook));
        permitCell.setCellValue(metrics.permitDays().size());

        addTooltip(factory, drawing, permitCell, row,
                "Giorni di permesso:\n" + metrics.permitDays().stream()
                        .map(d -> d.format(DATE_FORMATTER))
                        .collect(Collectors.joining("\n")));

        // Column 10: Ferie Residue
        Cell remainingVacationCell = row.createCell(col++);
        remainingVacationCell.setCellStyle(styles.getDecimalStyle(workbook));
        remainingVacationCell.setCellValue(metrics.remainingVacation());

        // Column 11: ROL Residui
        Cell remainingRolCell = row.createCell(col++);
        remainingRolCell.setCellStyle(styles.getDecimalStyle(workbook));
        remainingRolCell.setCellValue(metrics.remainingRol());
    }

    // ============= SHEET 2: DETTAGLIO GIORNALIERO =============

    private void createDailyDetailSheet(Workbook workbook, ExcelStyles styles,
                                        LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Dettaglio Giornaliero");

        // Larghezza colonne
        sheet.setColumnWidth(0, 12 * 256); // Data
        sheet.setColumnWidth(1, 25 * 256); // Dipendente
        sheet.setColumnWidth(2, 18 * 256); // Turno Previsto
        sheet.setColumnWidth(3, 12 * 256); // Entrata
        sheet.setColumnWidth(4, 12 * 256); // Uscita
        sheet.setColumnWidth(5, 10 * 256); // Ore
        sheet.setColumnWidth(6, 15 * 256); // Assenze
        sheet.setColumnWidth(7, 30 * 256); // Note

        // Header
        int currentRow = 0;
        Row headerRow = sheet.createRow(currentRow++);
        String[] headers = {
                "Data", "Dipendente", "Turno Previsto",
                "Entrata", "Uscita", "Ore", "Assenze", "Note"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.getBoldStyle(workbook));
        }

        // Dati giornalieri
        List<Employee> employees = employeeRepository.findAll(getCurrentUserCompanyId());
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            for (Employee employee : employees) {
                createDailyDetailRow(sheet, styles, workbook, currentRow++,
                        employee, currentDate);
            }
            currentDate = currentDate.plusDays(1);
        }

        logger.info("Sheet 'Dettaglio Giornaliero' creato");
    }

    private void createDailyDetailRow(Sheet sheet, ExcelStyles styles, Workbook workbook,
                                      int rowIndex, Employee employee, LocalDate date) {
    }

    // ============= SHEET 3: ASSENZE =============

    private void createAbsencesSheet(Workbook workbook, ExcelStyles styles,
                                     LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Assenze");

        // Larghezza colonne
        sheet.setColumnWidth(0, 25 * 256);
        for (int i = 1; i < 8; i++) {
            sheet.setColumnWidth(i, 15 * 256);
        }

        // Header
        int currentRow = 0;
        Row headerRow = sheet.createRow(currentRow++);
        String[] headers = {
                "Dipendente", "Tipo", "Dal", "Al",
                "Ore", "Stato", "Nota"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.getBoldStyle(workbook));
        }

        // Dati assenze
        List<Absence> absences = absenceRepository.findByDateRange(startDate, endDate, getCurrentUserCompanyId());

        for (Absence absence : absences) {
            Row row = sheet.createRow(currentRow++);
            int col = 0;

            Employee employee = employeeRepository.findById(absence.employeeId()).orElse(null);
            String employeeName = employee != null ?
                    employee.surname() + " " + employee.name() : "SCONOSCIUTO";

            row.createCell(col++).setCellValue(employeeName);
            row.createCell(col++).setCellValue(absence.type().name());
            row.createCell(col++).setCellValue(absence.startDate().format(DATE_FORMATTER));
            row.createCell(col++).setCellValue(absence.endDate().format(DATE_FORMATTER));
            row.createCell(col++).setCellValue(absence.hoursCount());
            row.createCell(col++).setCellValue(absence.status().name());
            row.createCell(col++).setCellValue(absence.note() != null ? absence.note() : "");
        }

        logger.info("Sheet 'Assenze' creato con {} record", absences.size());
    }

    // ============= SHEET 4: ANOMALIE =============

    private void createAnomaliesSheet(Workbook workbook, ExcelStyles styles,
                                      LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Anomalie");

        // Larghezza colonne
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 25 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 40 * 256);

        // Header
        int currentRow = 0;
        Row headerRow = sheet.createRow(currentRow++);
        String[] headers = {"Data", "Dipendente", "Tipo Anomalia", "Descrizione"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.getBoldStyle(workbook));
        }

        // Trova anomalie per tutti i dipendenti
        List<Employee> employees = employeeRepository.findAll(getCurrentUserCompanyId());

        for (Employee employee : employees) {
            LocalDate currentDate = startDate;

            while (!currentDate.isAfter(endDate)) {
                List<AccessLog> logs = accessRepository
                        .getLogsByEmployeeAndDate(employee.id(), currentDate);

                // Verifica anomalie
                if (!logs.isEmpty()) {
                    if (logs.size() % 2 != 0) {
                        // Numero dispari di timbrature
                        Row row = sheet.createRow(currentRow++);
                        row.createCell(0).setCellValue(currentDate.format(DATE_FORMATTER));
                        row.createCell(1).setCellValue(employee.surname() + " " + employee.name());
                        row.createCell(2).setCellValue("TIMBRATURA MANCANTE");
                        row.createCell(3).setCellValue("Numero dispari di timbrature");
                    }
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        logger.info("Sheet 'Anomalie' creato");
    }

    // ============= SHEET 5: SALDI FERIE/ROL =============

    private void createBalanceSheet(Workbook workbook, ExcelStyles styles,
                                    LocalDate startDate, LocalDate endDate) {
        Sheet sheet = workbook.createSheet("Saldi Ferie-ROL");

        // Larghezza colonne
        for (int i = 0; i < 9; i++) {
            sheet.setColumnWidth(i, 15 * 256);
        }

        // Header
        int currentRow = 0;
        Row headerRow = sheet.createRow(currentRow++);
        String[] headers = {
                "Dipendente", "Ferie Inizio", "Maturate", "Godute", "Residue",
                "ROL Inizio", "Maturati", "Goduti", "Residui"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.getBoldStyle(workbook));
        }

        // Dati saldi
        List<Employee> employees = employeeRepository.findAll(getCurrentUserCompanyId());

        for (Employee employee : employees) {
            Row row = sheet.createRow(currentRow++);
            int col = 0;

            EmployeeLeaveBalance balance = balanceRepository
                    .findByEmployeeId(employee.id())
                    .orElse(null);

            row.createCell(col++).setCellValue(employee.surname() + " " + employee.name());

            if (balance != null) {
                // Calcola ore godute nel periodo
                List<Absence> absences = absenceRepository
                        .findByEmployeeIdAndDateRange(employee.id(), startDate, endDate)
                        .stream()
                        .filter(a -> a.status() == AbsenceStatus.APPROVED)
                        .toList();

                int vacationUsed = absences.stream()
                        .filter(a -> a.type() == AbsenceType.VACATION)
                        .mapToInt(Absence::hoursCount)
                        .sum();

                int rolUsed = absences.stream()
                        .filter(a -> a.type() == AbsenceType.ROL)
                        .mapToInt(Absence::hoursCount)
                        .sum();

                // Ferie
                double vacationStart = balance.vacationAvailable().doubleValue() + vacationUsed;
                row.createCell(col++).setCellValue(vacationStart);
                row.createCell(col++).setCellValue(0); // TODO: calcolare maturazione
                row.createCell(col++).setCellValue(vacationUsed);
                row.createCell(col++).setCellValue(balance.vacationAvailable().doubleValue());

                // ROL
                double rolStart = balance.rolAvailable().doubleValue() + rolUsed;
                row.createCell(col++).setCellValue(rolStart);
                row.createCell(col++).setCellValue(0); // TODO: calcolare maturazione
                row.createCell(col++).setCellValue(rolUsed);
                row.createCell(col++).setCellValue(balance.rolAvailable().doubleValue());
            } else {
                for (int i = 0; i < 8; i++) {
                    row.createCell(col++).setCellValue(0);
                }
            }
        }

        logger.info("Sheet 'Saldi Ferie-ROL' creato");
    }

    // ============= UTILITY METHODS =============

    private void addTooltip(CreationHelper factory, Drawing<?> drawing,
                            Cell cell, Row row, String text) {
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setRow1(row.getRowNum());
        anchor.setCol2(cell.getColumnIndex() + 3);
        anchor.setRow2(row.getRowNum() + 5);

        Comment comment = drawing.createCellComment(anchor);
        comment.setString(factory.createRichTextString(text));
        comment.setAuthor("Sistema");
        cell.setCellComment(comment);
    }

    private String buildAbsenceTooltip(String type, Map<LocalDate, Integer> details) {
        if (details.isEmpty()) {
            return String.format("Nessun giorno di %s", type);
        }

        StringBuilder sb = new StringBuilder(String.format("%s:\n", type));
        for (Map.Entry<LocalDate, Integer> entry : details.entrySet()) {
            sb.append(String.format("- %s: %d ore\n",
                    entry.getKey().format(DATE_FORMATTER),
                    entry.getValue()));
        }
        return sb.toString();
    }

    private EmployeeMetrics calculateEmployeeMetrics(Long employeeId,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        // Giorni lavorati (con almeno un accesso)
        List<LocalDate> workedDays = accessRepository
                .getDistinctLogDates(employeeId, startDate, endDate);

        // Ore lavorate totali
        YearMonth yearMonth = YearMonth.from(startDate);
        int workedMinutes = accessService.calculateMonthlyWorkingHours(employeeId, yearMonth) * 60;
        double workedHours = workedMinutes / 60.0;

        // Ore previste dai turni
        List<ShiftAssignment> shifts = shiftAssignmentRepository
                .findByEmployeeIdAndDateRange(employeeId, startDate, endDate);

        int expectedMinutes = 0;
        for (ShiftAssignment shift : shifts) {
            if (!holidayRepository.isHoliday(shift.date())) {
                expectedMinutes += (int) ChronoUnit.MINUTES.between(
                        shift.startTime(), shift.endTime());
            }
        }
        double expectedHours = expectedMinutes / 60.0;

        // Assenze
        List<Absence> absences = absenceRepository
                .findByEmployeeIdAndDateRange(employeeId, startDate, endDate)
                .stream()
                .filter(a -> a.status() == AbsenceStatus.APPROVED)
                .toList();

        Map<LocalDate, Integer> vacationDetails = new HashMap<>();
        Map<LocalDate, Integer> rolDetails = new HashMap<>();
        Set<LocalDate> sickDays = new HashSet<>();
        Set<LocalDate> permitDays = new HashSet<>();

        for (Absence absence : absences) {
            switch (absence.type()) {
                case VACATION -> vacationDetails.put(absence.startDate(), absence.hoursCount());
                case ROL -> rolDetails.put(absence.startDate(), absence.hoursCount());
                case SICK_LEAVE -> {
                    LocalDate current = absence.startDate();
                    while (!current.isAfter(absence.endDate())) {
                        sickDays.add(current);
                        current = current.plusDays(1);
                    }
                }
                case PERMIT -> {
                    LocalDate current = absence.startDate();
                    while (!current.isAfter(absence.endDate())) {
                        permitDays.add(current);
                        current = current.plusDays(1);
                    }
                }
            }
        }

        double vacationHours = vacationDetails.values().stream()
                .mapToInt(Integer::intValue).sum();
        double rolHours = rolDetails.values().stream()
                .mapToInt(Integer::intValue).sum();

        // Saldi residui
        EmployeeLeaveBalance balance = balanceRepository
                .findByEmployeeId(employeeId)
                .orElse(null);

        double remainingVacation = balance != null ?
                balance.vacationAvailable().doubleValue() : 0;
        double remainingRol = balance != null ?
                balance.rolAvailable().doubleValue() : 0;

        return new EmployeeMetrics(
                workedDays,
                workedHours,
                expectedHours,
                vacationHours,
                rolHours,
                sickDays,
                permitDays,
                remainingVacation,
                remainingRol,
                vacationDetails,
                rolDetails
        );
    }

    private Long getCurrentUserCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getCompanyId();
    }

    // ============= INNER CLASSES =============

    private record EmployeeMetrics(
            List<LocalDate> workedDays,
            double workedHours,
            double expectedHours,
            double vacationHours,
            double rolHours,
            Set<LocalDate> sickDays,
            Set<LocalDate> permitDays,
            double remainingVacation,
            double remainingRol,
            Map<LocalDate, Integer> vacationDetails,
            Map<LocalDate, Integer> rolDetails
    ) {}
}