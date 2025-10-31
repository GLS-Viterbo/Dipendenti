package it.gls.dipendenti.hr.repository;

import it.gls.dipendenti.hr.model.EmployeeDocument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeDocumentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<EmployeeDocument> rowMapper = new EmployeeDocumentRowMapper();

    public EmployeeDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Add a new document to the database
     * @param document document to add
     * @return new document with id
     */
    public EmployeeDocument save(EmployeeDocument document) {
        String sql = """
                INSERT INTO employee_documents
                (employee_id, file_name, file_path, mime_type, description, uploaded_at, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        Long id = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                document.employeeId(),
                document.fileName(),
                document.filePath(),
                document.mimeType(),
                document.description(),
                Timestamp.from(document.uploadedAt()),
                document.deleted()
        );

        return new EmployeeDocument(
                id,
                document.employeeId(),
                document.fileName(),
                document.filePath(),
                document.mimeType(),
                document.description(),
                document.uploadedAt(),
                document.deleted()
        );
    }

    /**
     * Returns the document with the given id
     * @param id the id of the searched document
     * @return optional of document
     */
    public Optional<EmployeeDocument> findById(Long id) {
        String sql = "SELECT * FROM employee_documents WHERE id = ?";
        return jdbcTemplate.query(sql, rowMapper, id).stream().findFirst();
    }

    /**
     * Getting all documents for a specific employee
     * @param employeeId the id of the employee
     * @return list of documents
     */
    public List<EmployeeDocument> findByEmployeeId(Long employeeId) {
        String sql = "SELECT * FROM employee_documents WHERE employee_id = ? AND deleted = false ORDER BY uploaded_at DESC";
        return jdbcTemplate.query(sql, rowMapper, employeeId);
    }

    /**
     * Getting all documents that are not deleted
     * @return list of documents not deleted
     */
    public List<EmployeeDocument> findAll() {
        String sql = "SELECT * FROM employee_documents WHERE deleted = false ORDER BY uploaded_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Updates given document attributes
     * @param document document with changed attributes
     * @return true if changes have been made
     */
    public boolean update(EmployeeDocument document) {
        String sql = """
                UPDATE employee_documents
                SET employee_id = ?, file_name = ?, file_path = ?, mime_type = ?, description = ?, uploaded_at = ?, deleted = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(
                sql,
                document.employeeId(),
                document.fileName(),
                document.filePath(),
                document.mimeType(),
                document.description(),
                document.uploadedAt(),
                document.deleted(),
                document.id()
        );

        return rows > 0;
    }

    /**
     * Soft deleting a document
     * @param id document to delete
     * @return true if document is deleted
     */
    public boolean delete(Long id) {
        String sql = "UPDATE employee_documents SET deleted = true WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }

    /**
     * Count total documents for an employee
     * @param employeeId the id of the employee
     * @return total number of documents
     */
    public Long countByEmployeeId(Long employeeId) {
        String sql = "SELECT COUNT(*) FROM employee_documents WHERE employee_id = ? AND deleted = false";
        return jdbcTemplate.queryForObject(sql, Long.class, employeeId);
    }

    /**
     * Mapping database attributes to document
     */
    private static class EmployeeDocumentRowMapper implements RowMapper<EmployeeDocument> {
        @Override
        public EmployeeDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EmployeeDocument(
                    rs.getLong("id"),
                    rs.getLong("employee_id"),
                    rs.getString("file_name"),
                    rs.getString("file_path"),
                    rs.getString("mime_type"),
                    rs.getString("description"),
                    rs.getTimestamp("uploaded_at") != null ? rs.getTimestamp("uploaded_at").toInstant() : null,
                    rs.getBoolean("deleted")
            );
        }
    }
}