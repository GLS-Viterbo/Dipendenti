package it.gls.dipendenti.access.repository;

import it.gls.dipendenti.access.model.Card;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CardRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Card> rowMapper = new CardRowMapper();

    public CardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Adds a new card to the database
     * @param card the card to add
     * @return card with new id
     */
    public Card save(Card card) {
        String sql = "INSERT INTO cards (uid, deleted) VALUES (?,?) RETURNING id";
        Long id = jdbcTemplate.queryForObject(sql, Long.class, card.uid(), card.deleted());
        return new Card(id, card.uid(), card.deleted());
    }

    /**
     * Deletes a card
     * @param id id of the card to delete
     * @return true if success
     */
    public boolean deleteCard(Long id) {
        String sql = "UPDATE cards SET deleted = true WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }

    /**
     * Restores a deleted card
     * @param id id of the card to restore
     * @return true if success
     */
    public boolean restoreCard(Long id) {
        String sql = "UPDATE cards SET deleted = false WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }

    /**
     * Restores card with uid
     * @param id uid of the card to restore
     * @return true if success
     */
    public boolean restoreCardWithUid(Long uid) {
        String sql = "UPDATE cards SET deleted = false WHERE uid = ?";
        return jdbcTemplate.update(sql, uid) > 0;
    }

    /**
     * Return all card that are not deleted
     * @return all non-deleted cards
     */
    public List<Card> getAllCards() {
        String sql = "SELECT id, uid, deleted FROM cards WHERE deleted = false";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Long getCardCount() {
        String sql = "SELECT COUNT(*) FROM cards WHERE deleted = false";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    /**
     * Returns a list of deleted card
     * @return deleted cards
     */
    public List<Card> getDeletedCards() {
        String sql = "SELECT id, uid, deleted FROM cards WHERE deleted = true";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Returns a card with given id
     * @param id id of the card to get
     * @return Optional of card
     */
    public Optional<Card> getCardById(Long id) {
        List<Card> cards = jdbcTemplate.query("SELECT id, uid, deleted FROM cards WHERE id = ?", rowMapper, id);
        if (cards.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(cards.get(0));
        }
    }

    /**
     * Returns a card with given uid
     * @param uid uid to search
     * @return card
     */
    public Optional<Card> getCardByUid(String uid) {
        List<Card> cards = jdbcTemplate.query("SELECT id, uid, deleted FROM cards WHERE uid = ?", rowMapper, uid);
        if (cards.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(cards.get(0));
        }
    }

    public List<Card> getUnassignedCards() {
        String sql = """
            SELECT c.id, c.uid, c.deleted
            FROM cards c
            LEFT JOIN card_assignments ca
                   ON c.id = ca.card_id AND ca.end_date IS NULL
            WHERE ca.id IS NULL
            """;
        return jdbcTemplate.query(sql, rowMapper);
    }



    /**
     * Mapping card table attributes to new card object
     */
    private final static class CardRowMapper implements RowMapper<Card> {

        @Override
        public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Card(
                    rs.getLong("id"),
                    rs.getString("uid"),
                    rs.getBoolean("deleted")
            );
        }
    }

}
