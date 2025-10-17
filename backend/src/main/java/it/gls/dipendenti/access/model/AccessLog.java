package it.gls.dipendenti.access.model;

import java.time.Instant;
import java.time.ZonedDateTime;

public record AccessLog(
        Long id,
        Long employeeId,
        Long cardId,
        Instant timestamp,
        AccessType type,
        boolean modified,
        Instant modifiedAt,
        boolean deleted
) {
}
