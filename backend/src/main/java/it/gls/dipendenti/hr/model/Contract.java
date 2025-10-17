package it.gls.dipendenti.hr.model;

import java.time.LocalDate;

public record Contract(
   Long id,
   Long employeeId,
   LocalDate startDate,
   LocalDate endDate,
   int monthlyWorkingHours,
   boolean valid
) {}
