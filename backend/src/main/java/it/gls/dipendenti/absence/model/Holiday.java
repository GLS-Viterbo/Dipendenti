package it.gls.dipendenti.absence.model;

public record Holiday(
        Long id,
        String name,
        boolean recurring,
        short day,
        short month,
        short year,
        boolean deleted
) {}