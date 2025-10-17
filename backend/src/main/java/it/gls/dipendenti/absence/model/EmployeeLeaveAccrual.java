package it.gls.dipendenti.absence.model;

import java.math.BigDecimal;

public record EmployeeLeaveAccrual(
        Long id,
        Long employeeId,
        BigDecimal vacationHoursPerMonth,
        BigDecimal rolHoursPerMonth
) {}