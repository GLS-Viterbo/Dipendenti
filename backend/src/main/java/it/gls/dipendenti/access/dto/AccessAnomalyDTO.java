package it.gls.dipendenti.access.dto;

import java.time.LocalDate;

/**
 * DTO representing an access anomaly for an employee on a specific day
 */
public record AccessAnomalyDTO(
        Long employeeId,
        LocalDate date,
        AnomalyType type,
        String description,
        String employeeName
) {
    public enum AnomalyType {
        MISSING_EXIT,
        MISSING_ENTRY,
        ODD_NUMBER_LOGS
    }
}