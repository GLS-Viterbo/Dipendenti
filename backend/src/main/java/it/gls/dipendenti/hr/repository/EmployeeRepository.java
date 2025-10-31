package it.gls.dipendenti.hr.repository;

import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.util.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Employee> rowMapper = new EmployeeRowMapper();

    public EmployeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new employee to the database
     * @param employee employee to add
     * @return new employee with id
     */
    public Employee save(Employee employee) {
        String sql = """
                INSERT INTO employees
                (company_id, name, surname, tax_code, birthday, address, city, email, phone, note, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                employee.companyId(),
                employee.name(),
                employee.surname(),
                employee.taxCode(),
                employee.birthday(),
                employee.address(),
                employee.city(),
                employee.email(),
                employee.phone(),
                employee.note(),
                employee.deleted()
        );

        return new Employee(
                id,
                employee.companyId(),
                employee.name(),
                employee.surname(),
                employee.taxCode(),
                employee.birthday(),
                employee.address(),
                employee.city(),
                employee.email(),
                employee.phone(),
                employee.note(),
                employee.deleted()
        );
    }

    /**
     * Returns the employee with the given id
     * @param id the id of the searched employee
     * @return optional of employee
     */
    public Optional<Employee> findById(Long id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all employees that are not deleted
     * @return list of employees not deleted
     */
    public List<Employee> findAll(Long companyId) {
        String sql = "SELECT * FROM employees WHERE deleted = false AND company_id = ? ORDER BY name";
        return jdbcTemplate.query(sql, rowMapper, companyId);
    }

    /**
     * Getting all employees that are not deleted with pagination
     * @param page page number (0-based)
     * @param size number of elements per page
     * @return list of employees not deleted
     */
    public Page<Employee> findAll(int page, int size, Long companyId) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }
        String sql = """
        SELECT * FROM employees
        WHERE deleted = false AND company_id = ?
        ORDER BY surname
        LIMIT ? OFFSET ?
        """;
        int offset = page * size;
        List<Employee> content = jdbcTemplate.query(sql, rowMapper, companyId, size, offset);

        return new Page<>(content, page, size, countActive(companyId));
    }

    /**
     * Count total active employees (needed for pagination)
     * @return total number of active employees
     */
    public Long countActive(Long companyId) {
        String sql = "SELECT COUNT(*) FROM employees WHERE deleted = false AND company_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, companyId);
    }

    /**
     * Return and employee with give tax code
     * @param taxCode employee tax code
     * @return employee with given tax code
     */
    public Optional<Employee> findByTaxCode(String taxCode) {
        String sql = "SELECT * FROM employees WHERE LOWER(tax_code) = LOWER(?) AND deleted = false";
        return jdbcTemplate.query(sql, rowMapper, taxCode).stream().findFirst();
    }

    /**
     * Getting all deleted employees
     * @return list of deleted employees
     */
    public List<Employee> findAllDeleted() {
        String sql = "SELECT * FROM employees WHERE deleted = true";
        return jdbcTemplate.query(sql, rowMapper);
    }


    /**
     * Updates given employee attributes
     * @param employee employee with changed attributes
     * @return true if changes have been made
     */
    public boolean update(Employee employee) {
        String sql = """
                UPDATE employees
                SET company_id = ?, name = ?, surname = ?, tax_code = ?, birthday = ?, address = ?, city = ?, email = ?, phone = ?, note = ?, deleted = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                employee.companyId(),
                employee.name(),
                employee.surname(),
                employee.taxCode(),
                employee.birthday(),
                employee.address(),
                employee.city(),
                employee.email(),
                employee.phone(),
                employee.note(),
                employee.deleted(),
                employee.id()
        );

        return rows > 0;
    }

    /**
     * Deleting an employee
     * @param id employee to delete
     * @return true if employee is deleted
     */
    public boolean delete(Long id) {
        String sql = "UPDATE employees SET deleted = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    public List<Employee> getEmployeesWithoutCard(Long companyId) {
        String sql = """
        SELECT * FROM employees e
        WHERE NOT EXISTS (
            SELECT 1
            FROM card_assignments ca
            WHERE ca.employee_id = e.id
              AND ca.end_date IS NULL
        )
        AND e.deleted = false
        AND e.company_id = ?;
        """;
        return jdbcTemplate.query(sql, rowMapper, companyId);
    }

    /**
     * Mapping database attributes to employee
     */
    private static class EmployeeRowMapper implements RowMapper<Employee> {
        @Override
        public Employee mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Employee(
                    rs.getLong("id"),
                    rs.getLong("company_id"),
                    rs.getString("name"),
                    rs.getString("surname"),
                    rs.getString("tax_code"),
                    rs.getDate("birthday") != null ? rs.getDate("birthday").toLocalDate() : null,
                    rs.getString("address"),
                    rs.getString("city"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("note"),
                    rs.getBoolean("deleted")
            );
        }
    }
}
