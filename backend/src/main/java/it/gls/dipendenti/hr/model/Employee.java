package it.gls.dipendenti.hr.model;

import java.time.LocalDate;

public record Employee(
        Long id,
        Long companyId,
        String name,
        String surname,
        String taxCode,
        LocalDate birthday,
        String address,
        String city,
        String email,
        String phone,
        String note,
        boolean deleted
) {}
