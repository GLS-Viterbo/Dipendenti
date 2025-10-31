package it.gls.dipendenti.auth.model;

public record User(
        Long id,
        String username,
        String passwordHash,
        String email,
        Long companyId,
        boolean active
) {}
