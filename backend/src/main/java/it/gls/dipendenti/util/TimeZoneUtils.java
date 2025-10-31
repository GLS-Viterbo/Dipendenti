package it.gls.dipendenti.util;

import java.sql.Timestamp;
import java.time.*;

/**
 * Utility centralizzata per gestione timezone consistente
 * REGOLE:
 * - Storage DB: sempre TIMESTAMPTZ (UTC internamente)
 * - Application: sempre ZonedDateTime con COMPANY_ZONE
 * - API Input/Output: OffsetDateTime (ISO-8601 compliant)
 */
public final class TimeZoneUtils {

    public static final ZoneId COMPANY_ZONE = ZoneId.of("Europe/Rome");
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    private TimeZoneUtils() {
        throw new AssertionError("Utility class");
    }

    // ============= CURRENT TIME =============

    /**
     * Ottiene l'istante corrente (sempre UTC)
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * Ottiene l'ora corrente nel fuso aziendale
     */
    public static ZonedDateTime nowCompanyTime() {
        return ZonedDateTime.now(COMPANY_ZONE);
    }

    /**
     * Ottiene la data corrente nel fuso aziendale
     */
    public static LocalDate todayCompanyDate() {
        return LocalDate.now(COMPANY_ZONE);
    }

    // ============= CONVERSIONS TO INSTANT =============

    /**
     * Converte ZonedDateTime a Instant (per salvataggio DB)
     */
    public static Instant toInstant(ZonedDateTime zdt) {
        return zdt.toInstant();
    }

    /**
     * Converte OffsetDateTime a Instant (per salvataggio DB)
     */
    public static Instant toInstant(OffsetDateTime odt) {
        return odt.toInstant();
    }

    /**
     * Converte LocalDateTime (assume company timezone) a Instant
     */
    public static Instant toInstant(LocalDateTime ldt) {
        return ldt.atZone(COMPANY_ZONE).toInstant();
    }

    // ============= CONVERSIONS FROM INSTANT =============

    /**
     * Converte Instant a ZonedDateTime nel fuso aziendale
     */
    public static ZonedDateTime toCompanyTime(Instant instant) {
        return instant.atZone(COMPANY_ZONE);
    }

    /**
     * Converte Instant a OffsetDateTime nel fuso aziendale (per API response)
     */
    public static OffsetDateTime toCompanyOffsetDateTime(Instant instant) {
        return instant.atZone(COMPANY_ZONE).toOffsetDateTime();
    }

    /**
     * Converte Instant a LocalDate nel fuso aziendale
     */
    public static LocalDate toCompanyDate(Instant instant) {
        return instant.atZone(COMPANY_ZONE).toLocalDate();
    }

    /**
     * Converte Instant a LocalTime nel fuso aziendale
     */
    public static LocalTime toCompanyLocalTime(Instant instant) {
        return instant.atZone(COMPANY_ZONE).toLocalTime();
    }

    // ============= DATE RANGE HELPERS =============

    /**
     * Inizio giornata nel fuso aziendale (00:00:00)
     */
    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(COMPANY_ZONE).toInstant();
    }

    /**
     * Fine giornata nel fuso aziendale (23:59:59.999999999)
     */
    public static Instant endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(COMPANY_ZONE).minusNanos(1).toInstant();
    }

    /**
     * Combina data e ora nel fuso aziendale
     */
    public static Instant combine(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).atZone(COMPANY_ZONE).toInstant();
    }

    // ============= JDBC HELPERS =============

    /**
     * Converte Instant a Timestamp per JDBC (mantiene UTC)
     */
    public static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    /**
     * Converte Timestamp da JDBC a Instant
     */
    public static Instant fromTimestamp(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    /**
     * Converte ZonedDateTime a Timestamp per JDBC
     */
    public static Timestamp toTimestamp(ZonedDateTime zdt) {
        return zdt != null ? Timestamp.from(zdt.toInstant()) : null;
    }

    // ============= VALIDATION =============

    /**
     * Verifica se due istanti sono nello stesso giorno (fuso aziendale)
     */
    public static boolean isSameCompanyDay(Instant instant1, Instant instant2) {
        LocalDate date1 = toCompanyDate(instant1);
        LocalDate date2 = toCompanyDate(instant2);
        return date1.equals(date2);
    }

    /**
     * Verifica se un istante è oggi (fuso aziendale)
     */
    public static boolean isToday(Instant instant) {
        return toCompanyDate(instant).equals(todayCompanyDate());
    }
}