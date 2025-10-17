package it.gls.dipendenti.absence.repository;

import it.gls.dipendenti.absence.model.Holiday;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class HolidayRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Holiday> rowMapper = new HolidayRowMapper();

    public HolidayRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new holiday to the database
     * @param holiday holiday to add
     * @return new holiday with id
     */
    public Holiday save(Holiday holiday) {
        String sql = """
                INSERT INTO holiday (name, recurring, day, month, year, deleted)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                holiday.name(),
                holiday.recurring(),
                holiday.day(),
                holiday.month(),
                holiday.year(),
                holiday.deleted()
        );

        return new Holiday(
                id,
                holiday.name(),
                holiday.recurring(),
                holiday.day(),
                holiday.month(),
                holiday.year(),
                holiday.deleted()
        );
    }

    /**
     * Returns the holiday with the given id
     * @param id the id of the searched holiday
     * @return optional of holiday
     */
    public Optional<Holiday> findById(Long id) {
        String sql = "SELECT * FROM holiday WHERE id = ? AND deleted = false";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all holidays that are not deleted
     * @return list of holidays
     */
    public List<Holiday> findAll() {
        String sql = "SELECT * FROM holiday WHERE deleted = false ORDER BY month, day";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Getting all recurring holidays
     * @return list of recurring holidays
     */
    public List<Holiday> findAllRecurring() {
        String sql = "SELECT * FROM holiday WHERE recurring = true AND deleted = false ORDER BY month, day";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Getting holidays for a specific year
     * @param year the year
     * @return list of holidays
     */
    public List<Holiday> findByYear(short year) {
        String sql = """
                SELECT * FROM holiday
                WHERE (year = ? OR recurring = true)
                AND deleted = false
                ORDER BY month, day
                """;
        return jdbcTemplate.query(sql, rowMapper, year);
    }

    /**
     * Getting holidays in a date range
     * @param startDate start date
     * @param endDate end date
     * @return list of holidays
     */
    public List<Holiday> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT * FROM holiday
                WHERE deleted = false
                AND (
                    (recurring = true AND
                     ((month > ? OR (month = ? AND day >= ?)) AND
                      (month < ? OR (month = ? AND day <= ?))))
                    OR
                    (recurring = false AND year >= ? AND year <= ? AND
                     ((year > ? OR (year = ? AND month > ?) OR (year = ? AND month = ? AND day >= ?)) AND
                      (year < ? OR (year = ? AND month < ?) OR (year = ? AND month = ? AND day <= ?))))
                )
                ORDER BY year, month, day
                """;

        return jdbcTemplate.query(sql, rowMapper,
                startDate.getMonthValue(), startDate.getMonthValue(), startDate.getDayOfMonth(),
                endDate.getMonthValue(), endDate.getMonthValue(), endDate.getDayOfMonth(),
                startDate.getYear(), endDate.getYear(),
                startDate.getYear(), startDate.getYear(), startDate.getMonthValue(),
                startDate.getYear(), startDate.getMonthValue(), startDate.getDayOfMonth(),
                endDate.getYear(), endDate.getYear(), endDate.getMonthValue(),
                endDate.getYear(), endDate.getMonthValue(), endDate.getDayOfMonth()
        );
    }

    /**
     * Check if a specific date is a holiday
     * @param date the date to check
     * @return true if the date is a holiday
     */
    public boolean isHoliday(LocalDate date) {
        String sql = """
                SELECT COUNT(*) FROM holiday
                WHERE deleted = false
                AND (
                    (recurring = true AND month = ? AND day = ?)
                    OR
                    (recurring = false AND year = ? AND month = ? AND day = ?)
                )
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                date.getMonthValue(), date.getDayOfMonth(),
                date.getYear(), date.getMonthValue(), date.getDayOfMonth()
        );

        return count != null && count > 0;
    }

    /**
     * Find holiday by specific date
     * @param date the date
     * @return optional of holiday
     */
    public Optional<Holiday> findByDate(LocalDate date) {
        String sql = """
                SELECT * FROM holiday
                WHERE deleted = false
                AND (
                    (recurring = true AND month = ? AND day = ?)
                    OR
                    (recurring = false AND year = ? AND month = ? AND day = ?)
                )
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, rowMapper,
                date.getMonthValue(), date.getDayOfMonth(),
                date.getYear(), date.getMonthValue(), date.getDayOfMonth()
        ).stream().findFirst();
    }

    /**
     * Updates given holiday attributes
     * @param holiday holiday with changed attributes
     * @return true if changes have been made
     */
    public boolean update(Holiday holiday) {
        String sql = """
                UPDATE holiday
                SET name = ?, recurring = ?, day = ?, month = ?, year = ?, deleted = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                holiday.name(),
                holiday.recurring(),
                holiday.day(),
                holiday.month(),
                holiday.year(),
                holiday.deleted(),
                holiday.id()
        );

        return rows > 0;
    }

    /**
     * Soft deleting a holiday
     * @param id holiday id
     * @return true if holiday is deleted
     */
    public boolean delete(Long id) {
        String sql = "UPDATE holiday SET deleted = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Mapping database attributes to holiday
     */
    private static class HolidayRowMapper implements RowMapper<Holiday> {
        @Override
        public Holiday mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Holiday(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBoolean("recurring"),
                    rs.getShort("day"),
                    rs.getShort("month"),
                    rs.getShort("year"),
                    rs.getBoolean("deleted")
            );
        }
    }
}