package it.gls.dipendenti.access.model;

import java.time.LocalDate;

public record CardAssignment(
        Long id,
        Long employeeId,
        Long cardId,
        LocalDate startDate,
        LocalDate endDate
) {
}
