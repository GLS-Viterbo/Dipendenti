package it.gls.dipendenti.shift.service;

import it.gls.dipendenti.shift.exception.ShiftNotFoundException;
import it.gls.dipendenti.shift.exception.DuplicateShiftNameException;
import it.gls.dipendenti.shift.model.Shift;
import it.gls.dipendenti.shift.repository.ShiftRepository;
import it.gls.dipendenti.shift.repository.ShiftAssociationRepository;
import it.gls.dipendenti.shift.repository.ShiftAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssociationRepository associationRepository;
    private final ShiftAssignmentRepository assignmentRepository;

    public ShiftService(ShiftRepository shiftRepository,
                        ShiftAssociationRepository associationRepository,
                        ShiftAssignmentRepository assignmentRepository) {
        this.shiftRepository = shiftRepository;
        this.associationRepository = associationRepository;
        this.assignmentRepository = assignmentRepository;
    }

    /**
     * Creates a new shift
     * @param shift the shift to create
     * @return the created shift with id
     */
    @Transactional
    public Shift createShift(Shift shift) {
        validateShift(shift);

        if (shiftRepository.existsByName(shift.name())) {
            throw new DuplicateShiftNameException(shift.name());
        }

        return shiftRepository.save(new Shift(
                null,
                shift.name(),
                shift.startTime(),
                shift.endTime(),
                shift.active()
        ));
    }

    /**
     * Gets a shift by id
     * @param id the shift id
     * @return the shift
     */
    public Shift getShiftById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Shift id cannot be null");
        }
        return shiftRepository.findById(id)
                .orElseThrow(ShiftNotFoundException::new);
    }

    /**
     * Gets all shifts
     * @return list of all shifts
     */
    public List<Shift> getAllShifts() {
        return shiftRepository.findAll();
    }

    /**
     * Gets all active shifts
     * @return list of active shifts
     */
    public List<Shift> getAllActiveShifts() {
        return shiftRepository.findAllActive();
    }

    /**
     * Updates a shift
     * @param id the shift id
     * @param updatedShift the updated shift data
     * @return the updated shift
     */
    @Transactional
    public Shift updateShift(Long id, Shift updatedShift) {
        if (id == null) {
            throw new IllegalArgumentException("Shift id cannot be null");
        }

        Shift existingShift = shiftRepository.findById(id)
                .orElseThrow(ShiftNotFoundException::new);

        validateShift(updatedShift);

        // Check if name is being changed and if new name already exists
        if (!existingShift.name().equals(updatedShift.name()) &&
                shiftRepository.existsByName(updatedShift.name())) {
            throw new DuplicateShiftNameException(updatedShift.name());
        }

        Shift shift = new Shift(
                id,
                updatedShift.name(),
                updatedShift.startTime(),
                updatedShift.endTime(),
                updatedShift.active()
        );

        if (!shiftRepository.update(shift)) {
            throw new ShiftNotFoundException();
        }

        return shift;
    }

    /**
     * Toggles shift active status
     * @param id the shift id
     * @param active the new active status
     */
    @Transactional
    public void toggleShiftActiveStatus(Long id, boolean active) {
        if (id == null) {
            throw new IllegalArgumentException("Shift id cannot be null");
        }

        if (!shiftRepository.updateActiveStatus(id, active)) {
            throw new ShiftNotFoundException();
        }
    }

    /**
     * Deletes a shift and all its associations and assignments
     * @param id the shift id
     */
    @Transactional
    public void deleteShift(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Shift id cannot be null");
        }

        if (!shiftRepository.findById(id).isPresent()) {
            throw new ShiftNotFoundException();
        }

        // Delete all associations for this shift
        associationRepository.deleteByShiftId(id);

        // TODO Ricalcolare tutti i turni futuri lasciando però quelli inseriti manualmente

        if (!shiftRepository.delete(id)) {
            throw new ShiftNotFoundException();
        }
    }

    /**
     * Gets count of employees assigned to a shift
     * @param shiftId the shift id
     * @return count of employees
     */
    public int getEmployeeCount(Long shiftId) {
        if (shiftId == null) {
            throw new IllegalArgumentException("Shift id cannot be null");
        }
        return shiftRepository.countEmployeesWithShift(shiftId);
    }

    /**
     * Validates shift data
     */
    private void validateShift(Shift shift) {
        if (shift.name() == null || shift.name().isBlank()) {
            throw new IllegalArgumentException("Shift name cannot be null or empty");
        }

        if (shift.startTime() == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }

        if (shift.endTime() == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }

        if (shift.endTime().isBefore(shift.startTime()))
            throw new IllegalArgumentException("End time cannot be before start time");
    }
}