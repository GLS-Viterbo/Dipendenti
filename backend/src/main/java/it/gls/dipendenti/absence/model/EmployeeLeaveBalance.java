package it.gls.dipendenti.absence.model;

import java.math.BigDecimal;

public record EmployeeLeaveBalance(
        Long id,
        Long employeeId,
        BigDecimal vacationAvailable,
        BigDecimal rolAvailable
) {}