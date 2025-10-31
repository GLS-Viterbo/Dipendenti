package it.gls.dipendenti.auth.repository;

import it.gls.dipendenti.auth.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> rowMapper = new UserRowMapper();

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new user to the database
     * @param user user to add
     * @return new user with id
     */
    public User save(User user) {
        String sql = """
                INSERT INTO users
                (username, password_hash, email, company_id, active)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                user.username(),
                user.passwordHash(),
                user.email(),
                user.companyId(),
                user.active()
        );

        return new User(
                id,
                user.username(),
                user.passwordHash(),
                user.email(),
                user.companyId(),
                user.active()
        );
    }

    /**
     * Returns the user with the given id
     * @param id the id of the searched user
     * @return optional of user
     */
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Find user by username
     * @param username username
     * @return optional of user
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE LOWER(username) = LOWER(?)";
        return jdbcTemplate.query(sql, rowMapper, username).stream().findFirst();
    }


    /**
     * Getting all users
     * @return list of all users
     */
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY username";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Getting all active users
     * @return list of active users
     */
    public List<User> findAllActive() {
        String sql = "SELECT * FROM users WHERE active = true ORDER BY username";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Get all users by company id
     * @param companyId company id
     * @return list of users
     */
    public List<User> findByCompanyId(Long companyId) {
        String sql = "SELECT * FROM users WHERE company_id = ? ORDER BY username";
        return jdbcTemplate.query(sql, rowMapper, companyId);
    }

    /**
     * Updates given user attributes
     * @param user user with changed attributes
     * @return true if changes have been made
     */
    public boolean update(User user) {
        String sql = """
                UPDATE users
                SET username = ?, password_hash = ?, email = ?, company_id = ?, active = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                user.username(),
                user.passwordHash(),
                user.email(),
                user.companyId(),
                user.active(),
                user.id()
        );

        return rows > 0;
    }

    /**
     * Update password hash
     * @param userId user id
     * @param passwordHash new password hash
     * @return true if password is updated
     */
    public boolean updatePassword(Long userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, passwordHash, userId);
        return rows > 0;
    }

    /**
     * Deactivate a user
     * @param id user to deactivate
     * @return true if user is deactivated
     */
    public boolean deactivate(Long id) {
        String sql = "UPDATE users SET active = false WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Activate a user
     * @param id user to activate
     * @return true if user is activated
     */
    public boolean activate(Long id) {
        String sql = "UPDATE users SET active = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Delete a user
     * @param id user to delete
     * @return true if user is deleted
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Assign a role to a user
     * @param userId user id
     * @param roleId role id
     * @return true if role is assigned
     */
    public boolean assignRole(Long userId, Long roleId) {
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        int rows = jdbcTemplate.update(sql, userId, roleId);
        return rows > 0;
    }

    /**
     * Remove a role from a user
     * @param userId user id
     * @param roleId role id
     * @return true if role is removed
     */
    public boolean removeRole(Long userId, Long roleId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        int rows = jdbcTemplate.update(sql, userId, roleId);
        return rows > 0;
    }

    /**
     * Remove all roles from a user
     * @param userId user id
     * @return number of roles removed
     */
    public int removeAllRoles(Long userId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId);
    }

    /**
     * Check if user has a specific role
     * @param userId user id
     * @param roleId role id
     * @return true if user has the role
     */
    public boolean hasRole(Long userId, Long roleId) {
        String sql = "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, userId, roleId);
        return count != null && count > 0;
    }

    /**
     * Count total users
     * @return total number of users
     */
    public Long count() {
        String sql = "SELECT COUNT(*) FROM users";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Count active users
     * @return total number of active users
     */
    public Long countActive() {
        String sql = "SELECT COUNT(*) FROM users WHERE active = true";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Mapping database attributes to user
     */
    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("email"),
                    rs.getObject("company_id") != null ? rs.getLong("company_id") : null,
                    rs.getBoolean("active")
            );
        }
    }
}
