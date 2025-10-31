package it.gls.dipendenti.report.model;

public record MonthlyWorkingHoursStatsDTO(
        Integer hoursWorkedThisMonth,
        Integer absencesThisYear,
        Double attendanceRate
) {}
