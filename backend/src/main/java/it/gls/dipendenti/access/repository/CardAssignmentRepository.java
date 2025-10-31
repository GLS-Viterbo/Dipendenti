package it.gls.dipendenti.access.repository;

import it.gls.dipendenti.access.model.CardAssignment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CardAssignmentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<CardAssignment> rowMapper = new CardAssignmentMapper();

    public CardAssignmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates a new card assignment
     * @param cardAssignment card assignment to create
     * @return new card assignment with id
     */
    public CardAssignment save(CardAssignment cardAssignment) {
        String sql = "INSERT INTO card_assignments (employee_id, card_id, start_date, end_date) VALUES (?,?,?,?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class, cardAssignment.employeeId(),
                cardAssignment.cardId(), cardAssignment.startDate(), cardAssignment.endDate());

        return new CardAssignment(id, cardAssignment.employeeId(),
                cardAssignment.cardId(), cardAssignment.startDate(), cardAssignment.endDate());
    }

    /**
     * Adds end date to an assignment so card can be reused
     * @param assignmentId id of the assignment to revoke
     * @return true if changes are made
     */
    public boolean revokeNow(Long assignmentId) {
        String sql = "UPDATE card_assignments SET end_date = CURRENT_DATE WHERE id = ?";
        return jdbcTemplate.update(sql, assignmentId) > 0;
    }

    /**
     * Return true if card is assigned to someone
     * @param cardId id of card to check
     * @return boolean
     */
    public boolean isAssigned(Long cardId) {
        String sql = "SELECT id, employee_id, card_id, start_date, end_date FROM card_assignments WHERE card_id = ? AND end_date IS NULL";
        List<CardAssignment> assignments = jdbcTemplate.query(sql, rowMapper, cardId);
        return !assignments.isEmpty();
    }

    /**
     * Finds whose card it is
     * @param cardId id of the card
     * @return id of the employee who is associated with that card
     */
    public Long getAssignedEmployeeId(Long cardId) {
        String sql = "SELECT employee_id FROM card_assignments WHERE card_id = ? AND end_date IS NULL LIMIT 1";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong("employee_id"),
                cardId).stream().findFirst().orElse(null);
    }

    /**
     * Returns all active assignments of an employee
     * @param employeeId employee id
     * @return list of active assignments
     */
    public List<CardAssignment> getActiveAssignmentsByEmployee(Long employeeId) {
        String sql = "SELECT id, employee_id, card_id, start_date, end_date " +
                "FROM card_assignments WHERE employee_id = ? AND end_date IS NULL";
        return jdbcTemplate.query(sql, rowMapper, employeeId);
    }

    /**
     * Returns all the assignments of a card even ended ones
     * @param cardId card id
     * @return list of assignments
     */
    public List<CardAssignment> getHistoryByCard(Long cardId) {
        String sql = "SELECT id, employee_id, card_id, start_date, end_date " +
                "FROM card_assignments WHERE card_id = ? ORDER BY start_date DESC";
        return jdbcTemplate.query(sql, rowMapper, cardId);
    }


    /**
     * Find the active assignment of the card
     * @param cardId id of the card
     * @return assignment
     */
    public Optional<CardAssignment> getActiveAssignmentByCard(Long cardId) {
        String sql = "SELECT id, employee_id, card_id, start_date, end_date " +
                "FROM card_assignments WHERE card_id = ? AND end_date IS NULL";
        return jdbcTemplate.query(sql, rowMapper, cardId).stream().findFirst();
    }

    /**
     * Filters for last assignment record of every not deleted card and returns the count of cards without as end_date
     * @return cards with an active assignment
     */
    public Long getAssignedCards(Long companyId) {
        String sql = """
        SELECT COUNT(*) AS active_assignments_count
        FROM (
            SELECT DISTINCT ON (ca.card_id) ca.end_date
            FROM card_assignments ca
            JOIN cards c ON ca.card_id = c.id
            JOIN employees e ON ca.employee_id = e.id
            WHERE c.deleted = FALSE
            AND e.company_id = ?
            ORDER BY ca.card_id, ca.start_date DESC
        ) latest
        WHERE latest.end_date IS NULL;
        """;

        return jdbcTemplate.queryForObject(sql, Long.class, companyId);
    }


    private static final class CardAssignmentMapper implements RowMapper<CardAssignment> {

        @Override
        public CardAssignment mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new CardAssignment(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getLong("card_id"),
                    rs.getDate("start_date") == null ? null : rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate()
            );
        }
    }

}
