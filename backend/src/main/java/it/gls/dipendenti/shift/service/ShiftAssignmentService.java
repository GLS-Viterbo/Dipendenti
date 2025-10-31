package it.gls.dipendenti.shift.service;

import it.gls.dipendenti.absence.exception.OverlappingAbsenceException;
import it.gls.dipendenti.absence.model.Absence;
import it.gls.dipendenti.absence.model.AbsenceStatus;
import it.gls.dipendenti.absence.model.AbsenceType;
import it.gls.dipendenti.absence.repository.AbsenceRepository;
import it.gls.dipendenti.absence.repository.HolidayRepository;
import it.gls.dipendenti.auth.model.CustomUserDetails;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.shift.exception.OverlappingShiftException;
import it.gls.dipendenti.shift.exception.ShiftAssignmentNotFoundException;
import it.gls.dipendenti.shift.model.Shift;
import it.gls.dipendenti.shift.model.ShiftAssignment;
import it.gls.dipendenti.shift.model.ShiftAssociation;
import it.gls.dipendenti.shift.repository.ShiftAssignmentRepository;
import it.gls.dipendenti.shift.repository.ShiftAssociationRepository;
import it.gls.dipendenti.shift.repository.ShiftRepository;
import org.springframework.cglib.core.Local;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class ShiftAssignmentService {

    private final ShiftAssignmentRepository assignmentRepository;
    private final ShiftAssociationRepository associationRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final AbsenceRepository absenceRepository;
    private final HolidayRepository holidayRepository;

    public ShiftAssignmentService(ShiftAssignmentRepository assignmentRepository,
                                  ShiftAssociationRepository associationRepository,
                                  ShiftRepository shiftRepository,
                                  EmployeeRepository employeeRepository,
                                  AbsenceRepository absenceRepository,
                                  HolidayRepository holidayRepository) {
        this.assignmentRepository = assignmentRepository;
        this.holidayRepository = holidayRepository;
        this.associationRepository = associationRepository;
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
        this.absenceRepository = absenceRepository;
    }

    /**
     * Creates a manual shift assignment
     * @param assignment the assignment to create
     * @return the created assignment with id
     */
    @Transactional
    public ShiftAssignment createManualAssignment(ShiftAssignment assignment) {
        validateAssignment(assignment);

        // Check for overlapping shifts
        checkForOverlappingShifts(assignment);

        // Checking if shift in overlapping with an absence
        // checkForOverlappingAbsence(assignment);


        return assignmentRepository.save(new ShiftAssignment(
                null,
                assignment.employeeId(),
                assignment.date(),
                assignment.startTime(),
                assignment.endTime(),
                false, // manual assignment
                null,
                assignment.note()
        ));
    }

    /**
     * Gets an assignment by id
     * @param id the assignment id
     * @return the assignment
     */
    public ShiftAssignment getAssignmentById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Assignment id cannot be null");
        }
        return assignmentRepository.findById(id)
                .orElseThrow(ShiftAssignmentNotFoundException::new);
    }

    /**
     * Gets all assignments for an employee
     * @param employeeId the employee id
     * @return list of assignments
     */
    public List<ShiftAssignment> getEmployeeAssignments(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }

        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        return assignmentRepository.findByEmployeeId(employeeId);
    }

    /**
     * Gets all assignments for a specific date
     * @param date the date
     * @return list of assignments
     */
    public List<ShiftAssignment> getAssignmentsForDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return assignmentRepository.findByDate(date, getCurrentUserCompanyId());
    }

    /**
     * Gets assignment for an employee on a specific date
     * @param employeeId the employee id
     * @param date the date
     * @return the assignment if exists
     */
    public List<ShiftAssignment> getEmployeeAssignmentsForDate(Long employeeId, LocalDate date) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        return assignmentRepository.findByEmployeeId(employeeId).stream()
                .filter(a -> a.date().equals(date))
                .toList();
    }

    /**
     * Gets assignments in a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of assignments
     */
    public List<ShiftAssignment> getAssignmentsInRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        return assignmentRepository.findByDateRange(startDate, endDate, getCurrentUserCompanyId());
    }

    /**
     * Gets assignments for an employee in a date range
     * @param employeeId the employee id
     * @param startDate start date
     * @param endDate end date
     * @return list of assignments
     */
    public List<ShiftAssignment> getEmployeeAssignmentsInRange(Long employeeId, LocalDate startDate, LocalDate endDate) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        return assignmentRepository.findByEmployeeIdAndDateRange(employeeId, startDate, endDate);
    }

    /**
     * Updates an assignment
     * @param id the assignment id
     * @param updatedAssignment the updated assignment data
     * @return the updated assignment
     */
    @Transactional
    public ShiftAssignment updateAssignment(Long id, ShiftAssignment updatedAssignment) {
        if (id == null) {
            throw new IllegalArgumentException("Assignment id cannot be null");
        }

        ShiftAssignment existing = assignmentRepository.findById(id)
                .orElseThrow(ShiftAssignmentNotFoundException::new);

        validateAssignment(updatedAssignment);

        // Check for overlapping shifts (excluding current assignment)
        ShiftAssignment tempAssignment = new ShiftAssignment(
                id,
                existing.employeeId(),
                updatedAssignment.date(),
                updatedAssignment.startTime(),
                updatedAssignment.endTime(),
                updatedAssignment.autoGenerated(),
                updatedAssignment.modifiedAt(),
                updatedAssignment.note()
        );
        checkForOverlappingShifts(tempAssignment);

        ShiftAssignment assignment = new ShiftAssignment(
                id,
                updatedAssignment.employeeId(),
                updatedAssignment.date(),
                updatedAssignment.startTime(),
                updatedAssignment.endTime(),
                false,
                ZonedDateTime.now(), // mark as modified
                updatedAssignment.note()
        );

        if (!assignmentRepository.update(assignment)) {
            throw new ShiftAssignmentNotFoundException();
        }

        return assignment;
    }

    /**
     * Deletes an assignment
     * @param id the assignment id
     */
    @Transactional
    public void deleteAssignment(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Assignment id cannot be null");
        }

        if (!assignmentRepository.delete(id)) {
            throw new ShiftAssignmentNotFoundException();
        }
    }

    /**
     * Deletes all assignments for an employee
     * @param employeeId the employee id
     * @return number of deleted assignments
     */
    @Transactional
    public int deleteEmployeeAssignments(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }

        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        return assignmentRepository.deleteByEmployeeId(employeeId);
    }

    public int deleteFutureEmployeeAssignments(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }
        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new EmployeeNotFoundException();
        }
        return assignmentRepository.deleteFutureByEmployeeId(employeeId);
    }

    /**
     * Generates automatic assignments based on associations for a date range
     * @param startDate start date
     * @param endDate end date
     */
    public void generateAssignmentsForDateRange(LocalDate startDate, LocalDate endDate) {
        Long companyId = getCurrentUserCompanyId();
        generateAssignmentsForDateRange(startDate, endDate, companyId);
    }

    @Transactional
    public int generateAssignmentsForDateRange(LocalDate startDate, LocalDate endDate, Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }

        int totalGenerated = 0;

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            totalGenerated += generateAssignmentsForDate(currentDate, companyId);
            currentDate = currentDate.plusDays(1);
        }
        return totalGenerated;
    }

    public void generateAssignmentsForDate(LocalDate date) {
        Long companyId = getCurrentUserCompanyId();
        generateAssignmentsForDate(date, companyId);
    }

    /**
     * Generates automatic assignments for a specific date based on associations
     * @param date the date
     * @return number of assignments created
     */
    @Transactional
    public int generateAssignmentsForDate(LocalDate date, Long companyId) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID cannot be null");
        }

        if (holidayRepository.isHoliday(date))
            return 0;

        List<ShiftAssociation> associations = associationRepository.findByDayOfWeek(date.getDayOfWeek(), companyId);
        int createdCount = 0;

        for (ShiftAssociation association : associations) {

            Shift shift = shiftRepository.findById(association.shiftId()).orElse(null);
            if (shift == null || !shift.active()) {
                continue;
            }

            // Check for full-day absences (vacation or sick leave)
            List<Absence> absences = getApprovedAbsencesForEmployeeAndDate(
                    association.employeeId(), date);

            boolean hasFullDayAbsence = absences.stream()
                    .anyMatch(a -> (a.type() == AbsenceType.VACATION || a.type() == AbsenceType.SICK_LEAVE) &&
                            isFullDayAbsence(a, date));

            if (hasFullDayAbsence) {
                continue; // Skip this assignment
            }

            // Create assignment and adjust for partial absences
            ShiftAssignment assignment = new ShiftAssignment(
                    null,
                    association.employeeId(),
                    date,
                    shift.startTime(),
                    shift.endTime(),
                    true, // auto-generated
                    null,
                    null
            );

            try {
                checkForOverlappingShifts(assignment);
            } catch (OverlappingShiftException ex) {
                continue;
            }

            assignmentRepository.save(assignment);
            createdCount++;
        }

        return createdCount;
    }

    /**
     * Validates assignment data
     */
    private void validateAssignment(ShiftAssignment assignment) {
        if (assignment.employeeId() == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }

        if (assignment.date() == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        if (assignment.startTime() == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }

        if (assignment.endTime() == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }

        if(assignment.startTime().isAfter(assignment.endTime()))
            throw new IllegalArgumentException("Start date cannot be after end date!");

        // Verify employee exists
        if (employeeRepository.findById(assignment.employeeId()).isEmpty()) {
            throw new EmployeeNotFoundException();
        }
    }

    /**
     * Checks for overlapping shifts for the same employee on the same day
     */
    private void checkForOverlappingShifts(ShiftAssignment assignment) {
        List<ShiftAssignment> existingAssignments = assignmentRepository
                .findByEmployeeIdAndDate(assignment.employeeId(), assignment.date())
                .stream()
                .filter(a -> assignment.id() == null || !a.id().equals(assignment.id()))
                .toList();


        for (ShiftAssignment existing : existingAssignments) {
            if (shiftsOverlap(assignment.startTime(), assignment.endTime(),
                    existing.startTime(), existing.endTime())) {
                throw new OverlappingShiftException();
            }
        }
    }

    /**
     * Checks if a shift overlaps with an absence
     * @param assignment shift assignment
     */
    private void checkForOverlappingAbsence(ShiftAssignment assignment) {
        List<Absence> absences = absenceRepository.findByEmployeeIdAndDateRange(assignment.employeeId(), assignment.date(), assignment.date())
                .stream().filter(a -> a.status().equals(AbsenceStatus.APPROVED)).toList();
        for (Absence absence : absences) {
            System.out.println(absence);
            if (shiftsOverlap(assignment.startTime(), assignment.endTime(),
                    absence.startTime(), absence.endTime())) {
                throw new OverlappingAbsenceException();
            }
        }

    }

    /**
     * Adjusts shift assignment based on approved absences (ROL/PERMIT)
     * @param assignment the original assignment
     * @return adjusted assignment or null if entire shift is covered by absences
     */
    private ShiftAssignment adjustForAbsences(ShiftAssignment assignment) {
        List<Absence> absences = getApprovedAbsencesForEmployeeAndDate(assignment.employeeId(), assignment.date());

        LocalTime shiftStart = assignment.startTime();
        LocalTime shiftEnd = assignment.endTime();

        for (Absence absence : absences) {
            // If start time is not set it means that absence is for all day
            if (absence.startTime() == null || absence.endTime() == null) {
                return null;
            }
            LocalTime absenceStart = absence.startTime();
            LocalTime absenceEnd = absence.endTime();

            // Absence covers start of turn
            if (!shiftStart.isAfter(absenceEnd) && shiftStart.isBefore(absenceStart)) {
                shiftEnd = shiftEnd.isBefore(absenceEnd) ? shiftEnd : absenceStart;
            }

            // Absence covers end of turn
            if (!shiftEnd.isBefore(absenceStart) && shiftEnd.isAfter(absenceEnd)) {
                shiftStart = shiftStart.isAfter(absenceEnd) ? shiftStart : absenceEnd;
            }

            // absence covers all turn
            if (!shiftStart.isBefore(absenceStart) && !shiftEnd.isAfter(absenceEnd)) {
                return null;
            }

        }
        // Last check to see if new turn is valid
        if (!shiftStart.isBefore(shiftEnd)) {
            return null;
        }

        return new ShiftAssignment(
                assignment.id(),
                assignment.employeeId(),
                assignment.date(),
                shiftStart,
                shiftEnd,
                true,
                assignment.modifiedAt(),
                assignment.note()
        );
    }

    private List<Absence> getApprovedAbsencesForEmployeeAndDate(Long employeeId, LocalDate date) {
        return absenceRepository.findByEmployeeIdAndDateRange(employeeId, date, date).stream()
                .filter(a -> a.status().equals(AbsenceStatus.APPROVED))
                .toList();
    }

    private Long getCurrentUserCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getCompanyId();
    }

    private boolean isFullDayAbsence(Absence absence, LocalDate date) {
        return (absence.startTime() == null && absence.endTime() == null);
    }

    private boolean shiftsOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }
}
