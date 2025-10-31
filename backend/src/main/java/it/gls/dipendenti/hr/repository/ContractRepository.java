package it.gls.dipendenti.hr.repository;

import it.gls.dipendenti.hr.model.Contract;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ContractRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Contract> rowMapper = new ContractRowMapper();

    public ContractRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adding a new contract to the database
     * @param contract the contract to add
     * @return the contract created with the generated id
     */
    public Contract save(Contract contract) {
        String sql = "INSERT INTO contracts (employee_id, start_date, end_date, monthly_working_hours, valid) VALUES (?, ?, ?, ?, ?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class, contract.employeeId(), contract.startDate(), contract.endDate(),
                contract.monthlyWorkingHours(), contract.valid());
        return new Contract(id, contract.employeeId(), contract.startDate(), contract.endDate(), contract.monthlyWorkingHours(), contract.valid());
    }

    /**
     * Updates the attributes of the contract with the given id
     * @param contract new contract attributes
     * @return true if changes have been made
     */
    public boolean update(Contract contract) {
        String sql = "UPDATE contracts SET employee_id = ?, start_date = ?, end_date = ?, monthly_working_hours = ?, valid = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, contract.employeeId(), contract.startDate(), contract.endDate(),
                contract.monthlyWorkingHours(), contract.valid(), contract.id());
        return rows > 0;
    }

    /**
     * Search a contract with a given id
     * @param id id of contract to search
     * @return optional of contract
     */
    public Optional<Contract> getById(Long id) {
        String sql = "SELECT id, employee_id, start_date, end_date, monthly_working_hours, valid FROM contracts WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Search for an employee's active contract
     * @param id id of the employee
     * @return optional of contract
     */
    public Optional<Contract> getByEmployeeId(Long id) {
        String sql = "SELECT id, employee_id, start_date, end_date, monthly_working_hours, valid FROM contracts WHERE employee_id = ? AND valid = true";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    public List<Contract> getAllEmployeeContracts(Long employeeId) {
        String sql = "SELECT id, employee_id, start_date, end_date, monthly_working_hours, valid FROM contracts WHERE employee_id = ? ORDER BY start_date";
        return jdbcTemplate.query(sql, rowMapper, employeeId);
    }

    /**
     * Sets a contract to invalid
     * @param id the id of the contract
     * @return true if changes are made
     */
    public boolean invalidate(Long id) {
        LocalDate today = TimeZoneUtils.todayCompanyDate();
        String sql = "UPDATE contracts SET valid = false, end_date = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, today, id);
        return rows> 0;
    }

    /**
     * Mapping database attributes to new Contract
     */
    private static class ContractRowMapper implements RowMapper<Contract> {
        @Override
        public Contract mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Contract(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null,
                    rs.getInt("monthly_working_hours"),
                    rs.getBoolean("valid")
            );
        }
    }
}
