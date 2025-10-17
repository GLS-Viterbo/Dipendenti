package it.gls.dipendenti.access.repository;

import it.gls.dipendenti.access.model.AccessLog;
import it.gls.dipendenti.access.model.AccessType;
import it.gls.dipendenti.config.TimeZoneConstants;
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
        String sql = """
            SELECT id, employee_id, card_id, timestamp, type, modified, modified_at, deleted
            FROM access_logs
            WHERE employee_id = ? AND deleted = FALSE
            ORDER BY timestamp DESC
            LIMIT 1
            """;
        return jdbcTemplate.query(sql, rowMapper, employeeId).stream().findFirst();
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
        String sql = """
            SELECT type
            FROM access_logs
            WHERE employee_id = ?
              AND DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Europe/Rome') = 
                  DATE(? AT TIME ZONE 'Europe/Rome')
              AND deleted = FALSE
            ORDER BY timestamp DESC
            LIMIT 1
        """;

        try {
            String lastType = jdbcTemplate.queryForObject(
                    sql,
                    String.class,
                    employeeId,
                    Timestamp.from(now)
            );

            if ("IN".equalsIgnoreCase(lastType)) {
                return AccessType.OUT;
            } else {
                return AccessType.IN;
            }

        } catch (EmptyResultDataAccessException e) {
            // In case of first record of the day
            return AccessType.IN;
        }
    }



    /**
     * Get logs in a time range (timestamps in UTC)
     */
    public List<AccessLog> getLogsInTimeRange(Instant startTime, Instant endTime) {
        String sql = """
                SELECT id, employee_id, card_id, timestamp, type, modified, modified_at, deleted
                FROM access_logs
                WHERE timestamp BETWEEN ? AND ? AND deleted = false
                ORDER BY timestamp DESC""";
        return jdbcTemplate.query(sql, rowMapper,
                Timestamp.from(startTime),
                Timestamp.from(endTime));
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
     * @param zoneId the timezone to use for date calculation
     */
    public List<AccessLog> getLogsByDate(LocalDate date, ZoneId zoneId) {
        String sql = """
            SELECT id, employee_id, card_id, timestamp, type, modified, modified_at, deleted
            FROM access_logs
            WHERE DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE ?) = ?
            ORDER BY timestamp DESC
            """;
        return jdbcTemplate.query(sql, rowMapper, zoneId.getId(), date);
    }


    /**
     * Get all logs for an employee on a specific date
     * @param employeeId employee id
     * @param date the date to check
     * @return list of access logs for that day
     */
    public List<AccessLog> getLogsByEmployeeAndDate(Long employeeId, LocalDate date, ZoneId zoneId ) {
        String sql = """
            SELECT id, employee_id, card_id, timestamp, type, modified, modified_at, deleted
            FROM access_logs
            WHERE employee_id = ?
              AND DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE ?) = ?
              AND deleted = FALSE
            ORDER BY timestamp DESC
            """;
        return jdbcTemplate.query(sql, rowMapper, employeeId, zoneId.getId(), date);
    }

    /**
     * Get all distinct dates where an employee has logs in a date range
     * @param employeeId employee id
     * @param startDate start date
     * @param endDate end date
     * @return list of dates with access logs
     */
    public List<LocalDate> getDistinctLogDates(Long employeeId, LocalDate startDate, LocalDate endDate, ZoneId zoneId) {
        String sql = """
            SELECT DISTINCT DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE ?) as log_date
            FROM access_logs
            WHERE employee_id = ?
              AND DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE ?) BETWEEN ? AND ?
              AND deleted = FALSE
            ORDER BY log_date
            """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getDate("log_date").toLocalDate(),
                zoneId.getId(), employeeId, zoneId.getId(), startDate, endDate);
    }

    /**
     * Returns the count of employees with last read type as "IN"
     * Uses Europe/Rome timezone for "today"
     * @return employee count
     */
    public Long getEmployeesAtWork() {
        String sql = """
                WITH latest_access AS (
                    SELECT DISTINCT ON (employee_id) 
                           employee_id,
                           type,
                           timestamp
                    FROM access_logs
                    WHERE DATE(timestamp AT TIME ZONE 'UTC' AT TIME ZONE 'Europe/Rome') = 
                          CURRENT_DATE AT TIME ZONE 'Europe/Rome'
                      AND deleted = FALSE
                    ORDER BY employee_id, timestamp DESC
                )
                SELECT COUNT(*) AS employees_in
                FROM latest_access
                WHERE type = 'IN';
                """;
        return jdbcTemplate.queryForObject(sql, Long.class);
    }


    private final static class AccessRowMapper implements RowMapper<AccessLog> {

        @Override
        public AccessLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            Instant timestamp = rs.getTimestamp("timestamp").toInstant();
            Instant modifiedAt = rs.getTimestamp("modified_at") != null ?
                    rs.getTimestamp("modified_at").toInstant() : null;

            return new AccessLog(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getLong("card_id"),
                    timestamp,
                    AccessType.valueOf(rs.getString("type")),
                    rs.getBoolean("modified"),
                    modifiedAt,
                    rs.getBoolean("deleted")
            );
        }
    }
}
