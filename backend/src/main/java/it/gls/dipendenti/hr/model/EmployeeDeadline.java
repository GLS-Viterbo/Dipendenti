package it.gls.dipendenti.hr.model;

import java.time.LocalDate;

public record EmployeeDeadline(
        Long id,
        Long employeeId,
        String type,
        LocalDate expirationDate,
        String note,
        Integer reminderDays,
        String recipientEmail,
        boolean notified
) {}
