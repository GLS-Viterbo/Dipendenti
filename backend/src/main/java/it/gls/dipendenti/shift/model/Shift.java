package it.gls.dipendenti.shift.model;

import java.time.LocalTime;

public record Shift(
        Long id,
        String name,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
}
