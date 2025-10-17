package it.gls.dipendenti.access.dto;

import java.time.LocalDate;

public record CardWithDetails(
        Long id,
        String cardUid,
        Long employeeId,
        String employeeName,
        String employeeSurname,
        Long assignmentId,
        LocalDate assignmentStartDate,
        LocalDate assignmentEndDate,
        boolean active
) {}
