package it.gls.dipendenti.absence.model;

import java.time.*;

public record Absence(
        Long id,
        Long employeeId,
        AbsenceType type,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        int hoursCount,
        AbsenceStatus status,
        String note,
        Instant createdAt,
        boolean deleted
) {}