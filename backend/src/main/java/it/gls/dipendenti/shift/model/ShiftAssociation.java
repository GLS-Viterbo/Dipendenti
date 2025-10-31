package it.gls.dipendenti.shift.model;

import java.time.DayOfWeek;

public record ShiftAssociation (
        Long id,
        Long employeeId,
        Long shiftId,
        Integer dayOfWeek
) {}
