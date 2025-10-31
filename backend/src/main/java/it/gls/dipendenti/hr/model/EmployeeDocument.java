package it.gls.dipendenti.hr.model;

import java.time.Instant;

public record EmployeeDocument(
        Long id,
        Long employeeId,
        String fileName,
        String filePath,
        String mimeType,
        String description,
        Instant uploadedAt,
        boolean deleted
) {}