package it.gls.dipendenti.shift.service;

import it.gls.dipendenti.auth.model.CustomUserDetails;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.shift.exception.*;
import it.gls.dipendenti.shift.model.Shift;
import it.gls.dipendenti.shift.model.ShiftAssignment;
import it.gls.dipendenti.shift.model.ShiftAssociation;
import it.gls.dipendenti.shift.repository.ShiftAssociationRepository;
import it.gls.dipendenti.shift.repository.ShiftRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
public class ShiftAssociationService {

    private final ShiftAssociationRepository associationRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;

    public ShiftAssociationService(ShiftAssociationRepository associationRepository,
                                   ShiftRepository shiftRepository,
                                   EmployeeRepository employeeRepository) {
        this.associationRepository = associationRepository;
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Creates a new shift association
     * @param association the association to create
     * @return the created association with id
     */
    @Transactional
    public ShiftAssociation createAssociation(ShiftAssociation association) {
        validateAssociation(association);

        if(!shiftRepository.findById(association.shiftId()).get().active())
            throw new InvalidShiftException("Cannot assign an invalid shift");

        // Check if association already exists for this employee and day
        if (associationRepository.findByEmployeeIdDayOfWeekAndShift(
                association.employeeId(), association.dayOfWeek(), association.shiftId()).isPresent()) {
            throw new DuplicateShiftAssociationException();
        }

        checkForOverlappingShifts(association);

        return associationRepository.save(new ShiftAssociation(
                null,
                association.employeeId(),
                association.shiftId(),
                association.dayOfWeek()
        ));
    }

    /**
     * Gets an association by id
     * @param id the association id
     * @return the association
     */
    public ShiftAssociation getAssociationById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Association id cannot be null");
        }
        return associationRepository.findById(id)
                .orElseThrow(ShiftAssociationNotFoundException::new);
    }

    /**
     * Gets all associations for an employee
     * @param employeeId the employee id
     * @return list of associations
     */
    public List<ShiftAssociation> getEmployeeAssociations(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }

        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        return associationRepository.findByEmployeeId(employeeId);
    }

    /**
     * Gets association for an employee on a specific day
     * @param employeeId the employee id
     * @param dayOfWeek the day of week
     * @return the association if exists
     */
    public List<ShiftAssociation> getEmployeeAssociationForDay(Long employeeId, DayOfWeek dayOfWeek) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Day of week cannot be null");
        }

        return associationRepository.findByEmployeeIdAndDayOfWeek(employeeId, dayOfWeek);
    }

    /**
     * Gets all associations for a specific day
     * @param dayOfWeek the day of week
     * @return list of associations
     */
    public List<ShiftAssociation> getAssociationsForDay(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Day of week cannot be null");
        }
        return associationRepository.findByDayOfWeek(dayOfWeek, getCurrentUserCompanyId());
    }

    /**
     * Gets all employee ids assigned to a shift on a specific day
     * @param shiftId the shift id
     * @param dayOfWeek the day of week
     * @return list of employee ids
     */
    public List<Long> getEmployeesForShiftAndDay(Long shiftId, DayOfWeek dayOfWeek) {
        if (shiftId == null) {
            throw new IllegalArgumentException("Shift id cannot be null");
        }
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Day of week cannot be null");
        }

        if (shiftRepository.findById(shiftId).isEmpty()) {
            throw new ShiftNotFoundException();
        }

        return associationRepository.findEmployeeIdsByShiftIdAndDayOfWeek(shiftId, dayOfWeek);
    }

    /**
     * Gets all associations
     * @return list of all associations
     */
    public List<ShiftAssociation> getAllAssociations() {
        return associationRepository.findAll(getCurrentUserCompanyId());
    }

    /**
     * Deletes an association
     * @param id the association id
     */
    @Transactional
    public void deleteAssociation(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Association id cannot be null");
        }

        if (!associationRepository.delete(id)) {
            throw new ShiftAssociationNotFoundException();
        }
    }

    /**
     * Deletes all associations for an employee
     * @param employeeId the employee id
     * @return number of deleted associations
     */
    @Transactional
    public int deleteEmployeeAssociations(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }

        if (employeeRepository.findById(employeeId).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        return associationRepository.deleteByEmployeeId(employeeId);
    }

    /**
     * Checks for overlapping shifts for the same employee on the same day
     */
    private void checkForOverlappingShifts(ShiftAssociation association) {
        List<ShiftAssociation> existingAssociations = associationRepository
                .findByEmployeeIdAndDayOfWeek(association.employeeId(), DayOfWeek.of(association.dayOfWeek()))
                .stream()
                .filter(a -> association.id() == null || !a.id().equals(association.id()))
                .toList();


        for (ShiftAssociation existingAssociation : existingAssociations) {

            it.gls.dipendenti.shift.model.Shift newShift = shiftRepository.findById(association.shiftId()).orElseThrow(ShiftNotFoundException::new);
            it.gls.dipendenti.shift.model.Shift existingShift = shiftRepository.findById(existingAssociation.shiftId()).orElseThrow(ShiftNotFoundException::new);


            if (shiftsOverlap(newShift.startTime(), newShift.endTime(),
                    existingShift.startTime(), existingShift.endTime())) {
                throw new OverlappingShiftException();
            }
        }
    }

    private boolean shiftsOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    private Long getCurrentUserCompanyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getCompanyId();
    }

    /**
     * Validates association data
     */
    private void validateAssociation(ShiftAssociation association) {
        if (association.employeeId() == null) {
            throw new IllegalArgumentException("Employee id cannot be null");
        }

        if (association.shiftId() == null) {
            throw new IllegalArgumentException("Shift id cannot be null");
        }

        if (association.dayOfWeek() == null) {
            throw new IllegalArgumentException("Day of week cannot be null");
        }

        // Verify employee exists
        if (employeeRepository.findById(association.employeeId()).isEmpty()) {
            throw new EmployeeNotFoundException();
        }

        // Verify shift exists
        if (shiftRepository.findById(association.shiftId()).isEmpty()) {
            throw new ShiftNotFoundException();
        }
    }
}