package it.gls.dipendenti.auth.repository;

import it.gls.dipendenti.auth.model.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Role> rowMapper = new RoleRowMapper();

    public RoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new role to the database
     * @param role role to add
     * @return new role with id
     */
    public Role save(Role role) {
        String sql = """
                INSERT INTO roles (name)
                VALUES (?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(sql, Long.class, role.name());

        return new Role(id, role.name());
    }

    /**
     * Returns the role with the given id
     * @param id the id of the searched role
     * @return optional of role
     */
    public Optional<Role> findById(Long id) {
        String sql = "SELECT * FROM roles WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Find role by name
     * @param name role name
     * @return optional of role
     */
    public Optional<Role> findByName(String name) {
        String sql = "SELECT * FROM roles WHERE LOWER(name) = LOWER(?)";
        return jdbcTemplate.query(sql, rowMapper, name).stream().findFirst();
    }

    /**
     * Getting all roles
     * @return list of all roles
     */
    public List<Role> findAll() {
        String sql = "SELECT * FROM roles ORDER BY name";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Get all roles for a specific user
     * @param userId user id
     * @return list of roles
     */
    public List<Role> findByUserId(Long userId) {
        String sql = """
                SELECT r.* FROM roles r
                INNER JOIN user_roles ur ON r.id = ur.role_id
                WHERE ur.user_id = ?
                ORDER BY r.name
                """;
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    /**
     * Updates given role attributes
     * @param role role with changed attributes
     * @return true if changes have been made
     */
    public boolean update(Role role) {
        String sql = "UPDATE roles SET name = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, role.name(), role.id());
        return rows > 0;
    }

    /**
     * Delete a role
     * @param id role to delete
     * @return true if role is deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM roles WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Count total roles
     * @return total number of roles
     */
    public Long count() {
        String sql = "SELECT COUNT(*) FROM roles";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Mapping database attributes to role
     */
    private static class RoleRowMapper implements RowMapper<Role> {
        @Override
        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Role(
                    rs.getLong("id"),
                    rs.getString("name")
            );
        }
    }
}
