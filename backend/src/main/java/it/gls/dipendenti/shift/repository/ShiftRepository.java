package it.gls.dipendenti.shift.repository;

import it.gls.dipendenti.shift.model.Shift;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ShiftRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Shift> rowMapper = new ShiftRowMapper();

    public ShiftRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new shift to the database
     * @param shift shift to add
     * @return new shift with id
     */
    public Shift save(Shift shift) {
        String sql = """
                INSERT INTO shifts (name, start_time, end_time, active)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                shift.name(),
                shift.startTime(),
                shift.endTime(),
                shift.active()
        );

        return new Shift(
                id,
                shift.name(),
                shift.startTime(),
                shift.endTime(),
                shift.active()
        );
    }

    /**
     * Returns the shift with the given id
     * @param id the id of the searched shift
     * @return optional of shift
     */
    public Optional<Shift> findById(Long id) {
        String sql = "SELECT * FROM shifts WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all shifts
     * @return list of shifts
     */
    public List<Shift> findAll() {
        String sql = "SELECT * FROM shifts ORDER BY start_time";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Getting all active shifts
     * @return list of active shifts
     */
    public List<Shift> findAllActive() {
        String sql = "SELECT * FROM shifts WHERE active = true ORDER BY start_time";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Check if a shift with the given name already exists
     * @param name the shift name
     * @return true if exists
     */
    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM shifts WHERE name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name);
        return count != null && count > 0;
    }

    /**
     * Updates given shift attributes
     * @param shift shift with changed attributes
     * @return true if changes have been made
     */
    public boolean update(Shift shift) {
        String sql = """
                UPDATE shifts
                SET name = ?, start_time = ?, end_time = ?, active = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                shift.name(),
                shift.startTime(),
                shift.endTime(),
                shift.active(),
                shift.id()
        );

        return rows > 0;
    }

    /**
     * Toggle shift active status
     * @param id shift id
     * @param active new active status
     * @return true if changes have been made
     */
    public boolean updateActiveStatus(Long id, boolean active) {
        String sql = "UPDATE shifts SET active = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, active, id);
        return rows > 0;
    }

    /**
     * Deletes a shift
     * @param id shift id
     * @return true if shift is deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM shifts WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Count employees assigned to a specific shift
     * @param shiftId shift id
     * @return count of employees
     */
    public int countEmployeesWithShift(Long shiftId) {
        String sql = "SELECT COUNT(DISTINCT employee_id) FROM shift_associations WHERE shift_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, shiftId);
        return count != null ? count : 0;
    }

    /**
     * Mapping database attributes to shift
     */
    private static class ShiftRowMapper implements RowMapper<Shift> {
        @Override
        public Shift mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Shift(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getTime("start_time").toLocalTime(),
                    rs.getTime("end_time").toLocalTime(),
                    rs.getBoolean("active")
            );
        }
    }
}