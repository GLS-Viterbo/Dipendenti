package it.gls.dipendenti.access.repository;

import it.gls.dipendenti.access.model.AccessLog;
import it.gls.dipendenti.access.model.AccessType;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.*;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;

@Repository
public class AccessRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<AccessLog> rowMapper = new AccessRowMapper();

    public AccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate =jdbcTemplate;
    }

    public AccessLog addLog(AccessLog accessLog) {
        String sql = """
            INSERT INTO access_logs (employee_id, card_id, timestamp, type, modified, modified_at, deleted)
            VALUES (?,?,?,?,?,?,?) RETURNING id""";

        Timestamp timestamp = Timestamp.from(accessLog.timestamp());
        Timestamp modifiedAt = accessLog.modifiedAt() != null ?
                Timestamp.from(accessLog.modifiedAt()) : null;

        Long id = jdbcTemplate.queryForObject(sql, Long.class, accessLog.employeeId(), accessLog.cardId(),
                timestamp, accessLog.type().name(), accessLog.modified(), modifiedAt, accessLog.deleted());
        return new AccessLog(id, accessLog.employeeId(), accessLog.cardId(),
                accessLog.timestamp(), accessLog.type(), accessLog.modified(), accessLog.modifiedAt(), accessLog.deleted());
    }

    // Get the last access log for an employee (not deleted)
    public Optional<AccessLog> getLastLogByEmployee(Long employeeId) {
        LocalDate today = TimeZoneUtils.todayCompanyDate();
        Instant dayStart = TimeZoneUtils.startOfDay(today);
        Instant dayEnd = TimeZoneUtils.endOfDay(today);
        String sql = """
            SELECT id, employee_id, card_id, timestamp, type, modified, modified_at, deleted
            FROM access_logs
            WHERE employee_id = ? AND deleted = FALSE AND timestamp >= ? AND timestamp <= ?
            ORDER BY timestamp DESC
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rowMapper, employeeId, TimeZoneUtils.toTimestamp(dayStart), TimeZoneUtils.toTimestamp(dayEnd)).stream().findFirst();
    }

    public boolean modifyLog(AccessLog accessLog) {
        String sql = """
                UPDATE access_logs SET timestamp = ?, type = ?, modified = ?, modified_at = ?, deleted = ? WHERE id = ?
                """;
        return jdbcTemplate.update(sql, Timestamp.from(accessLog.timestamp()), accessLog.type().name(), accessLog.modified(),
                Timestamp.from(accessLog.modifiedAt()), accessLog.deleted(), accessLog.id()) > 0;
    }

    public boolean deleteLog(Long logId) {
        String sql = "UPDATE access_logs SET deleted = true WHERE id = ?";
        return jdbcTemplate.update(sql, logId) > 0;
    }

    public Optional<AccessLog> getById(Long logId) {
        String sql = """
                SELECT id, employee_id, card_id, timestamp, type, modified, modified_at, deleted
                FROM access_logs
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, rowMapper, logId).stream().findFirst();
    }

    // Based on previous access return the type of the next
    public AccessType getNextType(Long employeeId, Instant now) {
        // Converti l'istante corrente in data aziendale
        LocalDate companyDate = TimeZoneUtils.toCompanyDate(now);

        // Range della giornata nel fuso aziendale
        Instant dayStart = TimeZoneUtils.startOfDay(companyDate);
        Instant dayEnd = TimeZoneUtils.endOfDay(companyDate);

        String sql = """
            SELECT type FROM access_logs
            WHERE employee_id = ?
              AND timestamp >= ?
              AND timestamp <= ?
              AND deleted = FALSE
            ORDER BY timestamp DESC
            LIMIT 1
        """;

        try {
            String lastType = jdbcTemplate.queryForObject(sql, String.class,
                    employeeId,
                    TimeZoneUtils.toTimestamp(dayStart),
                    TimeZoneUtils.toTimestamp(dayEnd)
            );
            return "IN".equalsIgnoreCase(lastType) ? AccessType.OUT : AccessType.IN;
        } catch (EmptyResultDataAccessException e) {
            return AccessType.IN;
        }
    }



    /**
     * Get logs in a time range (timestamps in UTC)
     */
    public List<AccessLog> getLogsInTimeRange(Instant startTime, Instant endTime, Long companyId) {
        String sql = """
        SELECT al.* FROM access_logs al
        JOIN employees e ON al.employee_id = e.id
        WHERE al.timestamp BETWEEN ? AND ?
        AND e.company_id = ?
        AND al.deleted = false
        ORDER BY al.timestamp DESC
        """;
        return jdbcTemplate.query(sql, rowMapper,
                TimeZoneUtils.toTimestamp(startTime),
                TimeZoneUtils.toTimestamp(endTime),
                companyId);
    }

    public List<AccessLog> getLogsInTimeRangeByEmployee(Long employeeId, Instant startTime, Instant endTime) {
        String sql = """
                SELECT id, employee_id, card_id, timestamp, type, modified, modified_at, deleted
                FROM access_logs
                WHERE timestamp BETWEEN ? AND ?
                AND employee_id = ? AND deleted = false
                ORDER BY timestamp DESC""";
        return jdbcTemplate.query(sql, rowMapper, Timestamp.from(startTime), Timestamp.from(endTime), employeeId);
    }

    /**
     * Get all logs on a specific date in the given timezone
     * @param date the local date in the target timezone
     */
    public List<AccessLog> getLogsByDate(LocalDate date, Long companyId) {
        Instant dayStart = TimeZoneUtils.startOfDay(date);
        Instant dayEnd = TimeZoneUtils.endOfDay(date);

        String sql = """
        SELECT al.* FROM access_logs al
        JOIN employees e ON al.employee_id = e.id
        WHERE al.timestamp >= ? AND al.timestamp <= ?
        AND e.company_id = ?
        ORDER BY al.timestamp DESC
        """;
        return jdbcTemplate.query(sql, rowMapper,
                TimeZoneUtils.toTimestamp(dayStart),
                TimeZoneUtils.toTimestamp(dayEnd),
                companyId);
    }


    /**
     * Get all logs for an employee on a specific date
     * @param employeeId employee id
     * @param date the date to check
     * @return list of access logs for that day
     */
    public List<AccessLog> getLogsByEmployeeAndDate(Long employeeId, LocalDate date) {
        Instant dayStart = TimeZoneUtils.startOfDay(date);
        Instant dayEnd = TimeZoneUtils.endOfDay(date);

        String sql = """
            SELECT * FROM access_logs
            WHERE employee_id = ?
              AND timestamp >= ? AND timestamp <= ?
              AND deleted = FALSE
            ORDER BY timestamp DESC
        """;
        return jdbcTemplate.query(sql, rowMapper, employeeId,
                TimeZoneUtils.toTimestamp(dayStart),
                TimeZoneUtils.toTimestamp(dayEnd)
        );
    }

    /**
     * Get all distinct dates where an employee has logs in a date range
     * @param employeeId employee id
     * @param startDate start date
     * @param endDate end date
     * @return list of dates with access logs
     */
    public List<LocalDate> getDistinctLogDates(Long employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
        SELECT DISTINCT DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Europe/Rome') as log_date
        FROM access_logs
        WHERE employee_id = ?
          AND DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Europe/Rome') BETWEEN ? AND ?
          AND deleted = FALSE
        ORDER BY log_date
        """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getDate("log_date").toLocalDate(),
                employeeId, startDate, endDate);
    }

    /**
     * Returns the count of employees with last read type as "IN"
     * Uses Europe/Rome timezone for "today"
     * @return employee count
     */
    public Long getEmployeesAtWork(Long companyId) {
        LocalDate today = TimeZoneUtils.todayCompanyDate();
        Instant dayStart = TimeZoneUtils.startOfDay(today);
        Instant dayEnd = TimeZoneUtils.endOfDay(today);

        String sql = """
        WITH latest_access AS (
            SELECT DISTINCT ON (al.employee_id) 
                   al.employee_id,
                   al.type
            FROM access_logs al
            JOIN employees e ON al.employee_id = e.id
            WHERE al.timestamp >= ? AND al.timestamp <= ?
              AND e.company_id = ?
              AND al.deleted = FALSE
            ORDER BY al.employee_id, al.timestamp DESC
        )
        SELECT COUNT(*) AS employees_in
        FROM latest_access
        WHERE type = 'IN'
        """;

        return jdbcTemplate.queryForObject(sql, Long.class,
                TimeZoneUtils.toTimestamp(dayStart),
                TimeZoneUtils.toTimestamp(dayEnd),
                companyId);
    }


    private final static class AccessRowMapper implements RowMapper<AccessLog> {

        @Override
        public AccessLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AccessLog(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getLong("card_id"),
                    TimeZoneUtils.fromTimestamp(rs.getTimestamp("timestamp")),
                    AccessType.valueOf(rs.getString("type")),
                    rs.getBoolean("modified"),
                    TimeZoneUtils.fromTimestamp(rs.getTimestamp("modified_at")),
                    rs.getBoolean("deleted")
            );
        }
    }
}
