package it.gls.dipendenti.hr.repository;

import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.model.Group;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Group> rowMapper = new GroupRowMapper();

    public GroupRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adds a new group to the db
     * @param group new group
     * @return new group with id
     */
    public Group save(Group group) {
        String sql = "INSERT INTO employee_groups (name, deleted) VALUES (?, ?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class, group.name(), group.deleted());
        return new Group(id, group.name(), group.deleted());
    }

    /**
     * Returns group with given id
     * @param id the id of the group to find
     * @return Optional of group
     */
    public Optional<Group> findById(Long id) {
        String sql = "SELECT * FROM employee_groups WHERE id = ?";
        List<Group> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.stream().findFirst();
    }

    /**
     * Returns all groups
     * @return list of all groups
     */
    public List<Group> findAll() {
        String sql = "SELECT * FROM employee_groups WHERE deleted = false";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Update a group
     * @param group new group values with same id
     * @return true if changes have been made
     */
    public boolean update(Group group) {
        String sql = "UPDATE employee_groups SET name = ?, deleted = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, group.name(), group.deleted(), group.id());
        return rows > 0;
    }

    /**
     * Soft deleting a group
     * @param id the id of the group to delete
     * @return true if changes have been made
     */
    public boolean delete(Long id) {
        String sql = "UPDATE employee_groups SET deleted = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Adding an employee to a group
     * @param employeeId the id of the employee
     * @param groupId the id of the group
     * @return true if success
     */
    public boolean addMember(Long employeeId, Long groupId) {
        String sql = "INSERT INTO group_members (group_id, employee_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        return jdbcTemplate.update(sql, groupId, employeeId) > 0;
    }

    /**
     * Remove an employee from a group
     * @param employeeId employee id
     * @param groupId group id
     * @return true if successfull
     */
    public boolean removeMember(Long employeeId, Long groupId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND employee_id = ?";
        return jdbcTemplate.update(sql, groupId, employeeId) > 0;
    }

    /**
     * Check if an employee is member of a given group
     * @param employeeId the id of the employee
     * @param groupId the id of the group
     * @return true if it is part of the group
     */
    public boolean isMember(Long employeeId, Long groupId) {
        String sql = "SELECT employee_id FROM group_members WHERE group_id = ? AND employee_id = ?";
        List<Long> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("employee_id"),
                groupId,
                employeeId);
        return !rows.isEmpty();
    }

    /**
     * Fine the members of a group
     * @param groupId group id
     * @return list of employee id's
     */
    public List<Long> findMemberIds(Long groupId) {
        String sql = "SELECT employee_id FROM group_members WHERE group_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("employee_id"), groupId);
    }

    /**
     * Find groups of an employee
     * @param employeeId employee id
     * @return list of groups
     */
    public List<Long> findGroupIds(Long employeeId) {
        String sql = "SELECT group_id FROM group_members WHERE employee_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("group_id"), employeeId);
    }

    /**
     * Mapping database table attributes to a new Group
     */
    private static class GroupRowMapper implements RowMapper<Group> {
        @Override
        public Group mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Group(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBoolean("deleted")
            );
        }
    }
}
