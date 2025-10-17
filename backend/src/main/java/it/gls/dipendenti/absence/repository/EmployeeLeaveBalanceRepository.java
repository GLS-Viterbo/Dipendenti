package it.gls.dipendenti.absence.repository;

import it.gls.dipendenti.absence.model.EmployeeLeaveBalance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeLeaveBalanceRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<EmployeeLeaveBalance> rowMapper = new EmployeeLeaveBalanceRowMapper();

    public EmployeeLeaveBalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new leave balance for an employee
     * @param balance leave balance to add
     * @return new leave balance with id
     */
    public EmployeeLeaveBalance save(EmployeeLeaveBalance balance) {
        String sql = """
                INSERT INTO employee_leave_balance 
                (employee_id, vacation_available, rol_available)
                VALUES (?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                balance.employeeId(),
                balance.vacationAvailable(),
                balance.rolAvailable()
        );

        return new EmployeeLeaveBalance(
                id,
                balance.employeeId(),
                balance.vacationAvailable(),
                balance.rolAvailable()
        );
    }

    /**
     * Returns the leave balance with the given id
     * @param id the id of the searched leave balance
     * @return optional of leave balance
     */
    public Optional<EmployeeLeaveBalance> findById(Long id) {
        String sql = "SELECT * FROM employee_leave_balance WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Returns the leave balance for a specific employee
     * @param employeeId the employee id
     * @return optional of leave balance
     */
    public Optional<EmployeeLeaveBalance> findByEmployeeId(Long employeeId) {
        String sql = "SELECT * FROM employee_leave_balance WHERE employee_id = ?";
        return jdbcTemplate.query(sql, rowMapper, employeeId).stream().findFirst();
    }

    /**
     * Getting all leave balances
     * @return list of all leave balances
     */
    public List<EmployeeLeaveBalance> findAll() {
        String sql = "SELECT * FROM employee_leave_balance ORDER BY employee_id";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Check if an employee has a leave balance record
     * @param employeeId the employee id
     * @return true if balance exists
     */
    public boolean existsByEmployeeId(Long employeeId) {
        String sql = "SELECT COUNT(*) FROM employee_leave_balance WHERE employee_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, employeeId);
        return count != null && count > 0;
    }

    /**
     * Updates leave balance
     * @param balance leave balance with changed attributes
     * @return true if changes have been made
     */
    public boolean update(EmployeeLeaveBalance balance) {
        String sql = """
                UPDATE employee_leave_balance
                SET vacation_available = ?, rol_available = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                balance.vacationAvailable(),
                balance.rolAvailable(),
                balance.id()
        );

        return rows > 0;
    }

    /**
     * Add vacation hours to an employee's balance
     * @param employeeId employee id
     * @param hours hours to add
     * @return true if changes have been made
     */
    public boolean addVacationHours(Long employeeId, BigDecimal hours) {
        String sql = """
                UPDATE employee_leave_balance
                SET vacation_available = vacation_available + ?
                WHERE employee_id = ?
                """;

        int rows = jdbcTemplate.update(sql, hours, employeeId);
        return rows > 0;
    }

    /**
     * Subtract vacation hours from an employee's balance
     * @param employeeId employee id
     * @param days days to subtract
     * @return true if changes have been made
     */
    public boolean subtractVacationHours(Long employeeId, BigDecimal hours) {
        String sql = """
                UPDATE employee_leave_balance
                SET vacation_available = vacation_available - ?
                WHERE employee_id = ?
                """;

        int rows = jdbcTemplate.update(sql, hours, employeeId);
        return rows > 0;
    }

    /**
     * Add ROL hours to an employee's balance
     * @param employeeId employee id
     * @param hours hours to add
     * @return true if changes have been made
     */
    public boolean addRolHours(Long employeeId, BigDecimal hours) {
        String sql = """
                UPDATE employee_leave_balance
                SET rol_available = rol_available + ?
                WHERE employee_id = ?
                """;

        int rows = jdbcTemplate.update(sql, hours, employeeId);
        return rows > 0;
    }

    /**
     * Subtract ROL hours from an employee's balance
     * @param employeeId employee id
     * @param hours hours to subtract
     * @return true if changes have been made
     */
    public boolean subtractRolHours(Long employeeId, BigDecimal hours) {
        String sql = """
                UPDATE employee_leave_balance
                SET rol_available = rol_available - ?
                WHERE employee_id = ?
                """;

        int rows = jdbcTemplate.update(sql, hours, employeeId);
        return rows > 0;
    }

    /**
     * Check if employee has sufficient vacation hours
     * @param employeeId employee id
     * @param hours hours needed
     * @return true if sufficient balance
     */
    public boolean hasSufficientVacationDays(Long employeeId, BigDecimal hours) {
        String sql = "SELECT vacation_available FROM employee_leave_balance WHERE employee_id = ?";
        BigDecimal available = jdbcTemplate.queryForObject(sql, BigDecimal.class, employeeId);
        return available != null && available.compareTo(hours) >= 0;
    }

    /**
     * Check if employee has sufficient ROL hours
     * @param employeeId employee id
     * @param hours hours needed
     * @return true if sufficient balance
     */
    public boolean hasSufficientRolHours(Long employeeId, BigDecimal hours) {
        String sql = "SELECT rol_available FROM employee_leave_balance WHERE employee_id = ?";
        BigDecimal available = jdbcTemplate.queryForObject(sql, BigDecimal.class, employeeId);
        return available != null && available.compareTo(hours) >= 0;
    }

    /**
     * Get employees with low vacation balance
     * @param threshold minimum threshold
     * @return list of employee ids
     */
    public List<Long> findEmployeesWithLowVacationBalance(BigDecimal threshold) {
        String sql = """
                SELECT employee_id FROM employee_leave_balance
                WHERE vacation_available <= ?
                ORDER BY vacation_available
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("employee_id"), threshold);
    }

    /**
     * Get employees with negative balances
     * @return list of employee ids
     */
    public List<EmployeeLeaveBalance> findEmployeesWithNegativeBalance() {
        String sql = """
                SELECT * FROM employee_leave_balance 
                WHERE vacation_available < 0 OR rol_available < 0
                ORDER BY employee_id
                """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Reset vacation balance for an employee
     * @param employeeId employee id
     * @param newBalance new balance value
     * @return true if changes have been made
     */
    public boolean resetVacationBalance(Long employeeId, BigDecimal newBalance) {
        String sql = "UPDATE employee_leave_balance SET vacation_available = ? WHERE employee_id = ?";
        int rows = jdbcTemplate.update(sql, newBalance, employeeId);
        return rows > 0;
    }

    /**
     * Reset ROL balance for an employee
     * @param employeeId employee id
     * @param newBalance new balance value
     * @return true if changes have been made
     */
    public boolean resetRolBalance(Long employeeId, BigDecimal newBalance) {
        String sql = "UPDATE employee_leave_balance SET rol_available = ? WHERE employee_id = ?";
        int rows = jdbcTemplate.update(sql, newBalance, employeeId);
        return rows > 0;
    }

    /**
     * Deletes a leave balance record
     * @param id leave balance id
     * @return true if deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM employee_leave_balance WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Mapping database attributes to EmployeeLeaveBalance
     */
    private static class EmployeeLeaveBalanceRowMapper implements RowMapper<EmployeeLeaveBalance> {
        @Override
        public EmployeeLeaveBalance mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EmployeeLeaveBalance(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getBigDecimal("vacation_available"),
                    rs.getBigDecimal("rol_available")
            );
        }
    }
}