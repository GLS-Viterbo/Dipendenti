package it.gls.dipendenti.hr.model;

import java.util.List;

public record Group(
        Long id,
        String name,
        boolean deleted
) {}
