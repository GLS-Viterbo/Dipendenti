package it.gls.dipendenti.hr.repository;

import it.gls.dipendenti.hr.model.EmployeeDeadline;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeDeadlineRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<EmployeeDeadline> rowMapper = new EmployeeDeadlineRowMapper();

    public EmployeeDeadlineRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new deadline to the database
     * @param deadline deadline to add
     * @return new deadline with id
     */
    public EmployeeDeadline save(EmployeeDeadline deadline) {
        String sql = """
                INSERT INTO employee_deadlines
                (employee_id, type, expiration_date, note, reminder_days, recipient_email, notified)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                deadline.employeeId(),
                deadline.type(),
                deadline.expirationDate(),
                deadline.note(),
                deadline.reminderDays(),
                deadline.recipientEmail(),
                deadline.notified()
        );

        return new EmployeeDeadline(
                id,
                deadline.employeeId(),
                deadline.type(),
                deadline.expirationDate(),
                deadline.note(),
                deadline.reminderDays(),
                deadline.recipientEmail(),
                deadline.notified()
        );
    }

    /**
     * Returns the deadline with the given id
     * @param id the id of the searched deadline
     * @return optional of deadline
     */
    public Optional<EmployeeDeadline> findById(Long id) {
        String sql = "SELECT * FROM employee_deadlines WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all deadlines for a specific employee
     * @param employeeId the id of the employee
     * @return list of deadlines
     */
    public List<EmployeeDeadline> findByEmployeeId(Long employeeId) {
        String sql = "SELECT * FROM employee_deadlines WHERE employee_id = ? ORDER BY expiration_date";
        return jdbcTemplate.query(sql, rowMapper, employeeId);
    }

    /**
     * Getting all active deadlines (not expired)
     * @return list of active deadlines
     */
    public List<EmployeeDeadline> findAllActive() {
        String sql = "SELECT * FROM employee_deadlines WHERE expiration_date >= CURRENT_DATE ORDER BY expiration_date";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Getting all deadlines
     * @return list of all deadlines
     */
    public List<EmployeeDeadline> findAll() {
        String sql = "SELECT * FROM employee_deadlines ORDER BY expiration_date";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Getting deadlines expiring within a certain number of days
     * @param days number of days to look ahead
     * @return list of upcoming deadlines
     */
    public List<EmployeeDeadline> findUpcoming(int days) {
        String sql = """
                SELECT * FROM employee_deadlines
                WHERE expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE + (? * INTERVAL '1 day')
                ORDER BY expiration_date
                """;
        return jdbcTemplate.query(sql, rowMapper, days);
    }

    /**
     * Getting deadlines that need notification (within reminder_days and not yet notified)
     * @return list of deadlines needing notification
     */
    public List<EmployeeDeadline> findNeedingNotification() {
        String sql = """
                SELECT * FROM employee_deadlines
                WHERE expiration_date <= CURRENT_DATE + (reminder_days || ' days')::INTERVAL
                AND expiration_date >= CURRENT_DATE
                AND notified = false
                ORDER BY expiration_date
                """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Updates given deadline attributes
     * @param deadline deadline with changed attributes
     * @return true if changes have been made
     */
    public boolean update(EmployeeDeadline deadline) {
        String sql = """
                UPDATE employee_deadlines
                SET employee_id = ?, type = ?, expiration_date = ?, note = ?, reminder_days = ?, recipient_email = ?, notified = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                deadline.employeeId(),
                deadline.type(),
                deadline.expirationDate(),
                deadline.note(),
                deadline.reminderDays(),
                deadline.recipientEmail(),
                deadline.notified(),
                deadline.id()
        );

        return rows > 0;
    }

    /**
     * Mark a deadline as notified
     * @param id the id of the deadline
     * @return true if changes have been made
     */
    public boolean markAsNotified(Long id) {
        String sql = "UPDATE employee_deadlines SET notified = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Deleting a deadline
     * @param id deadline to delete
     * @return true if deadline is deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM employee_deadlines WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Count total deadlines for an employee
     * @param employeeId the id of the employee
     * @return total number of deadlines
     */
    public Long countByEmployeeId(Long employeeId) {
        String sql = "SELECT COUNT(*) FROM employee_deadlines WHERE employee_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, employeeId);
    }

    /**
     * Mapping database attributes to deadline
     */
    private static class EmployeeDeadlineRowMapper implements RowMapper<EmployeeDeadline> {
        @Override
        public EmployeeDeadline mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EmployeeDeadline(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getString("type"),
                    rs.getDate("expiration_date").toLocalDate(),
                    rs.getString("note"),
                    rs.getInt("reminder_days"),
                    rs.getString("recipient_email"),
                    rs.getBoolean("notified")
            );
        }
    }
}