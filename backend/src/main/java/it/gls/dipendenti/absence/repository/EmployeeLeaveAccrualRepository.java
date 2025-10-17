package it.gls.dipendenti.absence.repository;

import it.gls.dipendenti.absence.model.EmployeeLeaveAccrual;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeLeaveAccrualRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<EmployeeLeaveAccrual> rowMapper = new EmployeeLeaveAccrualRowMapper();

    public EmployeeLeaveAccrualRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new leave accrual configuration for an employee
     * @param accrual leave accrual to add
     * @return new leave accrual with id
     */
    public EmployeeLeaveAccrual save(EmployeeLeaveAccrual accrual) {
        String sql = """
                INSERT INTO employee_leave_accrual 
                (employee_id, vacation_hours_per_month, rol_hours_per_month)
                VALUES (?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                accrual.employeeId(),
                accrual.vacationHoursPerMonth(),
                accrual.rolHoursPerMonth()
        );

        return new EmployeeLeaveAccrual(
                id,
                accrual.employeeId(),
                accrual.vacationHoursPerMonth(),
                accrual.rolHoursPerMonth()
        );
    }

    /**
     * Returns the leave accrual with the given id
     * @param id the id of the searched leave accrual
     * @return optional of leave accrual
     */
    public Optional<EmployeeLeaveAccrual> findById(Long id) {
        String sql = "SELECT * FROM employee_leave_accrual WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Returns the leave accrual for a specific employee
     * @param employeeId the employee id
     * @return optional of leave accrual
     */
    public Optional<EmployeeLeaveAccrual> findByEmployeeId(Long employeeId) {
        String sql = "SELECT * FROM employee_leave_accrual WHERE employee_id = ?";
        return jdbcTemplate.query(sql, rowMapper, employeeId).stream().findFirst();
    }

    /**
     * Getting all leave accrual configurations
     * @return list of all leave accruals
     */
    public List<EmployeeLeaveAccrual> findAll() {
        String sql = "SELECT * FROM employee_leave_accrual ORDER BY employee_id";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Check if an employee has a leave accrual configuration
     * @param employeeId the employee id
     * @return true if configuration exists
     */
    public boolean existsByEmployeeId(Long employeeId) {
        String sql = "SELECT COUNT(*) FROM employee_leave_accrual WHERE employee_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, employeeId);
        return count != null && count > 0;
    }

    /**
     * Updates leave accrual configuration
     * @param accrual leave accrual with changed attributes
     * @return true if changes have been made
     */
    public boolean update(EmployeeLeaveAccrual accrual) {
        String sql = """
                UPDATE employee_leave_accrual
                SET vacation_hours_per_month = ?, rol_hours_per_month = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                accrual.vacationHoursPerMonth(),
                accrual.rolHoursPerMonth(),
                accrual.id()
        );

        return rows > 0;
    }

    /**
     * Deletes a leave accrual configuration
     * @param id leave accrual id
     * @return true if deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM employee_leave_accrual WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Mapping database attributes to EmployeeLeaveAccrual
     */
    private static class EmployeeLeaveAccrualRowMapper implements RowMapper<EmployeeLeaveAccrual> {
        @Override
        public EmployeeLeaveAccrual mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EmployeeLeaveAccrual(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getBigDecimal("vacation_hours_per_month"),
                    rs.getBigDecimal("rol_hours_per_month")
            );
        }
    }
}