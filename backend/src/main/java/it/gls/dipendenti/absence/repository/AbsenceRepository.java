package it.gls.dipendenti.absence.repository;

import it.gls.dipendenti.absence.model.Absence;
import it.gls.dipendenti.absence.model.AbsenceStatus;
import it.gls.dipendenti.absence.model.AbsenceType;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AbsenceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Absence> rowMapper = new AbsenceRowMapper();

    public AbsenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new absence to the database
     * @param absence absence to add
     * @return new absence with id
     */
    public Absence save(Absence absence) {
        String sql = """
            INSERT INTO absence
            (employee_id, type, start_date, end_date, start_time, end_time,
            hours_count, status, note, created_at, deleted)
            VALUES (?, ?::VARCHAR, ?, ?, ?, ?, ?, ?::VARCHAR, ?, ?, ?)
            RETURNING id
            """;

        Long id = jdbcTemplate.queryForObject(sql, Long.class,
                absence.employeeId(),
                absence.type().name(),
                absence.startDate(),
                absence.endDate(),
                absence.startTime(),
                absence.endTime(),
                absence.hoursCount(),
                absence.status().name(),
                absence.note(),
                TimeZoneUtils.toTimestamp(absence.createdAt()), // ✅ Fix
                absence.deleted()
        );

        return new Absence(id, absence.employeeId(), absence.type(),
                absence.startDate(), absence.endDate(), absence.startTime(),
                absence.endTime(), absence.hoursCount(), absence.status(),
                absence.note(), absence.createdAt(), absence.deleted());
    }

    /**
     * Returns the absence with the given id
     * @param id the id of the searched absence
     * @return optional of absence
     */
    public Optional<Absence> findById(Long id) {
        String sql = "SELECT * FROM absence WHERE id = ? AND deleted = false";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all absences for a specific employee
     * @param employeeId the employee id
     * @return list of absences
     */
    public List<Absence> findByEmployeeId(Long employeeId, Long companyId) {
        String sql = """
        SELECT a.* FROM absence a
        JOIN employees e ON a.employee_id = e.id
        WHERE a.employee_id = ? AND e.company_id = ? AND a.deleted = false 
        ORDER BY a.start_date DESC
        """;
        return jdbcTemplate.query(sql, rowMapper, employeeId, companyId);
    }

    /**
     * Getting all absences for a specific employee in a date range
     * @param employeeId the employee id
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of absences
     */
    public List<Absence> findByEmployeeIdAndDateRange(Long employeeId,
                                                      LocalDate startDate,
                                                      LocalDate endDate) {
        String sql = """
            SELECT * FROM absence
            WHERE employee_id = ?
            AND deleted = false
            AND start_date <= ?
            AND end_date >= ?
            ORDER BY start_date DESC
            """;
        return jdbcTemplate.query(sql, rowMapper, employeeId, endDate, startDate);
    }


    /**
     * Getting all absences by type for a specific employee
     * @param employeeId the employee id
     * @param type the absence type
     * @return list of absences
     */
    public List<Absence> findByEmployeeIdAndType(Long employeeId, AbsenceType type) {
        String sql = """
                SELECT * FROM absence
                WHERE employee_id = ?
                AND type = ?::VARCHAR
                AND deleted = false
                ORDER BY start_date DESC
                """;
        return jdbcTemplate.query(sql, rowMapper, employeeId, type.name());
    }

    /**
     * Getting all absences in a date range
     * @param startDate start date of the range
     * @param endDate end date of the range
     * @return list of absences
     */
    public List<Absence> findByDateRange(LocalDate startDate, LocalDate endDate, Long companyId) {
        String sql = """
        SELECT a.* FROM absence a
        JOIN employees e ON a.employee_id = e.id
        WHERE a.deleted = false
        AND e.company_id = ?
        AND a.start_date <= ?
        AND a.end_date >= ?
        ORDER BY a.start_date DESC
        """;
        return jdbcTemplate.query(sql, rowMapper, companyId, endDate, startDate);
    }


    /**
     * Updates given absence attributes
     * @param absence absence with changed attributes
     * @return true if changes have been made
     */
    public boolean update(Absence absence) {
        String sql = """
                UPDATE absence
                SET employee_id = ?, type = ?::VARCHAR, start_date = ?, end_date = ?, 
                    start_time = ?, end_time = ?, hours_count = ?, 
                    status = ?::VARCHAR, note = ?, deleted = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                absence.employeeId(),
                absence.type().name(),
                absence.startDate(),
                absence.endDate(),
                absence.startTime(),
                absence.endTime(),
                absence.hoursCount(),
                absence.status().name(),
                absence.note(),
                absence.deleted(),
                absence.id()
        );

        return rows > 0;
    }

    /**
     * Update only the status of an absence
     * @param id absence id
     * @param status new status
     * @return true if changes have been made
     */
    public boolean updateStatus(Long id, AbsenceStatus status) {
        String sql = "UPDATE absence SET status = ?::VARCHAR WHERE id = ?";
        int rows = jdbcTemplate.update(sql, status.name(), id);
        return rows > 0;
    }

    /**
     * Soft deleting an absence
     * @param id absence id
     * @return true if absence is deleted
     */
    public boolean delete(Long id) {
        String sql = "UPDATE absence SET deleted = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Get number of approved absences today
     * @return approved absences of today
     */
    public Long getTodayCount(Long companyId) {
        String sql = """
        SELECT COUNT(DISTINCT a.employee_id) AS approved_absences_today
        FROM absence a
        JOIN employees e ON a.employee_id = e.id
        WHERE a.deleted = FALSE
          AND e.company_id = ?
          AND a.status = 'APPROVED'
          AND a.start_date <= CURRENT_DATE
          AND a.end_date >= CURRENT_DATE;
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, companyId);
    }

    /**
     * Get the number of future absences to approve
     * @return absences to approve
     */
    public Long getToApproveCount(Long companyId) {
        String sql = """
        SELECT COUNT(a.employee_id) as to_approve
        FROM absence a
        JOIN employees e ON a.employee_id = e.id
        WHERE a.deleted = FALSE
            AND e.company_id = ?
            AND a.start_date >= CURRENT_DATE
            AND a.status = 'PENDING'
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, companyId);
    }

    /**
     * Check if there are overlapping absences for an employee
     * @param employeeId the employee id
     * @param startDate start date
     * @param endDate end date
     * @param excludeId optional absence id to exclude from check (for updates)
     * @return true if overlapping absences exist
     */
    public boolean hasOverlappingAbsences(Long employeeId, LocalDate startDate,
                                          LocalDate endDate, Long excludeId) {
        String sql = """
            SELECT COUNT(*) FROM absence
            WHERE employee_id = ?
            AND deleted = false
            AND status != 'REJECTED'
            AND id != ?
            AND start_date <= ?
            AND end_date >= ?
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, employeeId,
                excludeId != null ? excludeId : -1L, endDate, startDate);
        return count != null && count > 0;
    }

    /**
     * Mapping database attributes to absence
     */
    private static class AbsenceRowMapper implements RowMapper<Absence> {
        @Override
        public Absence mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Absence(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    AbsenceType.valueOf(rs.getString("type")),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate(),
                    rs.getTime("start_time") != null ? rs.getTime("start_time").toLocalTime() : null,
                    rs.getTime("end_time") != null ? rs.getTime("end_time").toLocalTime() : null,
                    rs.getInt("hours_count"),
                    AbsenceStatus.valueOf(rs.getString("status")),
                    rs.getString("note"),
                    TimeZoneUtils.fromTimestamp(rs.getTimestamp("created_at")), // ✅ Fix
                    rs.getBoolean("deleted")
            );
        }
    }
}