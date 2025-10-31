package it.gls.dipendenti.shift.repository;

import it.gls.dipendenti.shift.model.ShiftAssociation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public class ShiftAssociationRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<ShiftAssociation> rowMapper = new ShiftAssociationRowMapper();

    public ShiftAssociationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new shift association to the database
     * @param association shift association to add
     * @return new shift association with id
     */
    public ShiftAssociation save(ShiftAssociation association) {
        String sql = """
                INSERT INTO shift_associations (employee_id, shift_id, day_of_week)
                VALUES (?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                association.employeeId(),
                association.shiftId(),
                association.dayOfWeek()
        );

        return new ShiftAssociation(
                id,
                association.employeeId(),
                association.shiftId(),
                association.dayOfWeek()
        );
    }

    /**
     * Returns the shift association with the given id
     * @param id the id of the searched shift association
     * @return optional of shift association
     */
    public Optional<ShiftAssociation> findById(Long id) {
        String sql = "SELECT * FROM shift_associations WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all shift associations for a specific employee
     * @param employeeId the employee id
     * @return list of shift associations
     */
    public List<ShiftAssociation> findByEmployeeId(Long employeeId) {
        String sql = "SELECT * FROM shift_associations WHERE employee_id = ? ORDER BY day_of_week";
        return jdbcTemplate.query(sql, rowMapper, employeeId);
    }

    /**
     * Getting shift association for a specific employee on a specific day
     * @param employeeId the employee id
     * @param dayOfWeek the day of week
     * @return optional of shift association
     */
    public Optional<ShiftAssociation> findByEmployeeIdDayOfWeekAndShift(Long employeeId, Integer dayOfWeek, Long shiftId) {
        String sql = "SELECT * FROM shift_associations WHERE employee_id = ? AND day_of_week = ? AND shift_id = ?";
        return jdbcTemplate.query(sql, rowMapper, employeeId, dayOfWeek, shiftId).stream().findFirst();
    }

    /**
     * Getting shift associations for a specific employee on a specific day
     * @param employeeId the employee id
     * @param dayOfWeek the day of week
     * @return list of associations
     */
    public List<ShiftAssociation> findByEmployeeIdAndDayOfWeek(Long employeeId, DayOfWeek dayOfWeek) {
        String sql = "SELECT * FROM shift_associations WHERE employee_id = ? AND day_of_week = ?";
        return jdbcTemplate.query(sql, rowMapper, employeeId, dayOfWeek.getValue()).stream().toList();
    }

    /**
     * Getting all shift associations for a specific day of week
     * @param dayOfWeek the day of week
     * @return list of shift associations
     */
    public List<ShiftAssociation> findByDayOfWeek(DayOfWeek dayOfWeek, Long companyId) {
        String sql = """
        SELECT sa.* FROM shift_associations sa
        JOIN employees e ON sa.employee_id = e.id
        WHERE sa.day_of_week = ? AND e.company_id = ?
        ORDER BY sa.employee_id
        """;
        return jdbcTemplate.query(sql, rowMapper, dayOfWeek.getValue(), companyId);
    }

    /**
     * Getting all employees assigned to a shift on a specific day
     * @param shiftId the shift id
     * @param dayOfWeek the day of week
     * @return list of employee ids
     */
    public List<Long> findEmployeeIdsByShiftIdAndDayOfWeek(Long shiftId, DayOfWeek dayOfWeek) {
        String sql = "SELECT employee_id FROM shift_associations WHERE shift_id = ? AND day_of_week = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("employee_id"), shiftId, dayOfWeek.getValue());
    }

    /**
     * Getting all shift associations
     * @return list of all shift associations
     */
    public List<ShiftAssociation> findAll(Long companyId) {
        String sql = """
        SELECT sa.* FROM shift_associations sa
        JOIN employees e ON sa.employee_id = e.id
        WHERE e.company_id = ?
        ORDER BY sa.employee_id, sa.day_of_week
        """;
        return jdbcTemplate.query(sql, rowMapper, companyId);
    }

    /**
     * Updates given shift association attributes
     * @param association shift association with changed attributes
     * @return true if changes have been made
     */
    public boolean update(ShiftAssociation association) {
        String sql = """
                UPDATE shift_associations
                SET employee_id = ?, shift_id = ?, day_of_week = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                association.employeeId(),
                association.shiftId(),
                association.dayOfWeek(),
                association.id()
        );

        return rows > 0;
    }

    /**
     * Deletes a shift association
     * @param id shift association id
     * @return true if association is deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM shift_associations WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Deletes all shift associations for an employee
     * @param employeeId the employee id
     * @return number of deleted associations
     */
    public int deleteByEmployeeId(Long employeeId) {
        String sql = "DELETE FROM shift_associations WHERE employee_id = ?";
        return jdbcTemplate.update(sql, employeeId);
    }

    /**
     * Deletes all shift associations for a shift
     * @param shiftId the shift id
     * @return number of deleted associations
     */
    public int deleteByShiftId(Long shiftId) {
        String sql = "DELETE FROM shift_associations WHERE shift_id = ?";
        return jdbcTemplate.update(sql, shiftId);
    }

    /**
     * Deletes shift association for an employee on a specific day
     * @param employeeId the employee id
     * @param dayOfWeek the day of week
     * @return true if association is deleted
     */
    public boolean deleteByEmployeeIdAndDayOfWeek(Long employeeId, DayOfWeek dayOfWeek) {
        String sql = "DELETE FROM shift_associations WHERE employee_id = ? AND day_of_week = ?";
        int rows = jdbcTemplate.update(sql, employeeId, dayOfWeek.getValue());
        return rows > 0;
    }

    /**
     * Mapping database attributes to shift association
     */
    private static class ShiftAssociationRowMapper implements RowMapper<ShiftAssociation> {
        @Override
        public ShiftAssociation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ShiftAssociation(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getLong("shift_id"),
                    rs.getInt("day_of_week")
            );
        }
    }
}