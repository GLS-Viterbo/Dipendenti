package it.gls.dipendenti.hr.repository;

import it.gls.dipendenti.hr.model.Company;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CompanyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Company> rowMapper = new CompanyRowMapper();

    public CompanyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new company to the database
     * @param company company to add
     * @return new company with id
     */
    public Company save(Company company) {
        String sql = """
                INSERT INTO companies
                (name, active)
                VALUES (?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                company.name(),
                company.active()
        );

        return new Company(
                id,
                company.name(),
                company.active()
        );
    }

    /**
     * Returns the company with the given id
     * @param id the id of the searched company
     * @return optional of company
     */
    public Optional<Company> findById(Long id) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all companies
     * @return list of all companies
     */
    public List<Company> findAll() {
        String sql = "SELECT * FROM companies ORDER BY name";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Getting all active companies
     * @return list of active companies
     */
    public List<Company> findAllActive() {
        String sql = "SELECT * FROM companies WHERE active = true ORDER BY name";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Find company by name
     * @param name company name
     * @return optional of company
     */
    public Optional<Company> findByName(String name) {
        String sql = "SELECT * FROM companies WHERE LOWER(name) = LOWER(?)";
        return jdbcTemplate.query(sql, rowMapper, name).stream().findFirst();
    }

    /**
     * Updates given company attributes
     * @param company company with changed attributes
     * @return true if changes have been made
     */
    public boolean update(Company company) {
        String sql = """
                UPDATE companies
                SET name = ?, active = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                company.name(),
                company.active(),
                company.id()
        );

        return rows > 0;
    }

    /**
     * Deactivate a company
     * @param id company to deactivate
     * @return true if company is deactivated
     */
    public boolean deactivate(Long id) {
        String sql = "UPDATE companies SET active = false WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Activate a company
     * @param id company to activate
     * @return true if company is activated
     */
    public boolean activate(Long id) {
        String sql = "UPDATE companies SET active = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Delete a company (only if no employees are associated)
     * @param id company to delete
     * @return true if company is deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM companies WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Count employees associated with a company
     * @param companyId the id of the company
     * @return total number of employees
     */
    public Long countEmployees(Long companyId) {
        String sql = "SELECT COUNT(*) FROM employees WHERE company_id = ? AND deleted = false";
        return jdbcTemplate.queryForObject(sql, Long.class, companyId);
    }

    /**
     * Count total companies
     * @return total number of companies
     */
    public Long count() {
        String sql = "SELECT COUNT(*) FROM companies";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Mapping database attributes to company
     */
    private static class CompanyRowMapper implements RowMapper<Company> {
        @Override
        public Company mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Company(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBoolean("active")
            );
        }
    }
}