package it.gls.dipendenti.config;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TimeZoneConstants {
    public static final ZoneId COMPANY_ZONE = ZoneId.of("Europe/Rome");
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    private TimeZoneConstants() {
        // Prevent instantiation
    }

    public static ZonedDateTime nowCompanyTime() {
        return ZonedDateTime.now(COMPANY_ZONE);
    }

    public static ZonedDateTime toCompanyTime(ZonedDateTime dateTime) {
        return dateTime.withZoneSameInstant(COMPANY_ZONE);
    }

    public static ZonedDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(COMPANY_ZONE);
    }

    public static ZonedDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(COMPANY_ZONE).minusNanos(1);
    }
}