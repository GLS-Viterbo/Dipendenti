package it.gls.dipendenti.absence.service;

import it.gls.dipendenti.absence.exception.HolidayNotFoundException;
import it.gls.dipendenti.absence.exception.DuplicateHolidayException;
import it.gls.dipendenti.absence.model.Holiday;
import it.gls.dipendenti.absence.repository.HolidayRepository;
import it.gls.dipendenti.shift.repository.ShiftAssignmentRepository;
import it.gls.dipendenti.shift.service.ShiftAssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final ShiftAssignmentService assignmentService;

    public HolidayService(HolidayRepository holidayRepository,
                          ShiftAssignmentRepository assignmentRepository,
                          ShiftAssignmentService assignmentService) {
        this.holidayRepository = holidayRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
    }

    /**
     * Creates a new holiday and removes shift assignments for that date
     * @param holiday the holiday to create
     * @return the created holiday with id
     */
    @Transactional
    public Holiday createHoliday(Holiday holiday) {
        validateHoliday(holiday);

        // Check for duplicate holidays on the same date
        LocalDate holidayDate = buildHolidayDate(holiday);
        if (holidayRepository.isHoliday(holidayDate)) {
            throw new DuplicateHolidayException(
                    "A holiday already exists for date: " + holidayDate
            );
        }

        // Create the holiday
        Holiday savedHoliday = holidayRepository.save(new Holiday(
                null,
                holiday.name(),
                holiday.recurring(),
                holiday.day(),
                holiday.month(),
                holiday.year(),
                false
        ));

        // Remove existing shift assignments for this date if it's in the future
        if (!holidayDate.isBefore(LocalDate.now())) {
            deleteShiftAssignmentsForHoliday(savedHoliday);
        }

        return savedHoliday;
    }

    /**
     * Gets a holiday by id
     * @param id the holiday id
     * @return the holiday
     */
    public Holiday getHolidayById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Holiday id cannot be null");
        }
        return holidayRepository.findById(id)
                .orElseThrow(() -> new HolidayNotFoundException("Holiday not found with id: " + id));
    }

    /**
     * Gets all holidays
     * @return list of all holidays
     */
    public List<Holiday> getAllHolidays() {
        return holidayRepository.findAll();
    }

    /**
     * Gets all recurring holidays
     * @return list of recurring holidays
     */
    public List<Holiday> getRecurringHolidays() {
        return holidayRepository.findAllRecurring();
    }

    /**
     * Gets holidays in a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of holidays
     */
    public List<Holiday> getHolidaysByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        return holidayRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Checks if a specific date is a holiday
     * @param date the date to check
     * @return true if holiday
     */
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return holidayRepository.isHoliday(date);
    }

    /**
     * Soft deletes a holiday and regenerates shift assignments for that date
     * @param id the holiday id
     */
    @Transactional
    public void deleteHoliday(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Holiday id cannot be null");
        }

        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new HolidayNotFoundException("Holiday not found with id: " + id));

        if (!holidayRepository.delete(id)) {
            throw new HolidayNotFoundException("Failed to delete holiday with id: " + id);
        }

        // Regenerate shift assignments for the date that's no longer a holiday
        LocalDate holidayDate = buildHolidayDate(holiday);

        if (holiday.recurring()) {
            // For recurring holidays, regenerate for current and next year
            int currentYear = LocalDate.now().getYear();
            for (int year = currentYear; year <= currentYear + 1; year++) {
                LocalDate date = LocalDate.of(year, holiday.month(), holiday.day());
                if (!date.isBefore(LocalDate.now())) {
                    assignmentService.generateAssignmentsForDate(date);
                }
            }
        } else {
            // For non-recurring holidays, regenerate only if in the future
            if (!holidayDate.isBefore(LocalDate.now())) {
                assignmentService.generateAssignmentsForDate(holidayDate);
            }
        }
    }

    /**
     * Validates holiday data
     */
    private void validateHoliday(Holiday holiday) {
        if (holiday.name() == null || holiday.name().isBlank()) {
            throw new IllegalArgumentException("Holiday name cannot be null or empty");
        }

        if (holiday.name().length() > 100) {
            throw new IllegalArgumentException("Holiday name cannot exceed 100 characters");
        }

        if (holiday.day() < 1 || holiday.day() > 31) {
            throw new IllegalArgumentException("Day must be between 1 and 31");
        }

        if (holiday.month() < 1 || holiday.month() > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }

        // Validate that day is valid for the given month
        try {
            LocalDate.of(holiday.year() != 0 ? holiday.year() : 2024,
                    holiday.month(),
                    holiday.day());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid date: day " + holiday.day() + " is not valid for month " + holiday.month()
            );
        }

        // For non-recurring holidays, year must be set and reasonable
        if (!holiday.recurring()) {
            if (holiday.year() == 0) {
                throw new IllegalArgumentException(
                        "Year must be specified for non-recurring holidays"
                );
            }
            if (holiday.year() < 1900 || holiday.year() > 2100) {
                throw new IllegalArgumentException(
                        "Year must be between 1900 and 2100"
                );
            }
        }
    }

    /**
     * Deletes shift assignments for a holiday date
     */
    private void deleteShiftAssignmentsForHoliday(Holiday holiday) {
        if (holiday.recurring()) {
            // For recurring holidays, delete for current and next year
            int currentYear = LocalDate.now().getYear();
            for (int year = currentYear; year <= currentYear + 1; year++) {
                LocalDate date = LocalDate.of(year, holiday.month(), holiday.day());
                if (!date.isBefore(LocalDate.now())) {
                    assignmentRepository.deleteByDateRange(date, date);
                }
            }
        } else {
            // For non-recurring holidays, delete only the specific date
            LocalDate date = LocalDate.of(holiday.year(), holiday.month(), holiday.day());
            assignmentRepository.deleteByDateRange(date, date);
        }
    }

    /**
     * Builds a LocalDate from a Holiday
     */
    private LocalDate buildHolidayDate(Holiday holiday) {
        int year = holiday.recurring() ? LocalDate.now().getYear() : holiday.year();
        return LocalDate.of(year, holiday.month(), holiday.day());
    }
}