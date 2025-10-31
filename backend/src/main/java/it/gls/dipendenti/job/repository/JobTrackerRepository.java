package it.gls.dipendenti.job.repository;

import it.gls.dipendenti.job.model.JobTracker;
import it.gls.dipendenti.job.model.JobTracker.JobType;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JobTrackerRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<JobTracker> rowMapper = new JobTrackerRowMapper();

    public JobTrackerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<JobTracker> findByJobName(String jobName) {
        String sql = "SELECT * FROM job_tracker WHERE job_name = ?";
        return jdbcTemplate.query(sql, rowMapper, jobName).stream().findFirst();
    }

    public List<JobTracker> findAllEnabled() {
        String sql = "SELECT * FROM job_tracker WHERE enabled = TRUE ORDER BY next_scheduled_run_date";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public List<JobTracker> findOverdueJobs(Instant now) {
        String sql = """
            SELECT * FROM job_tracker
            WHERE enabled = TRUE
            AND next_scheduled_run_date < ?
            ORDER BY next_scheduled_run_date
            """;
        return jdbcTemplate.query(sql, rowMapper, TimeZoneUtils.toTimestamp(now));
    }

    public boolean updateAfterSuccess(String jobName, Instant executionTime, Instant nextRun) {
        String sql = """
            UPDATE job_tracker
            SET last_successful_run_date = ?,
                next_scheduled_run_date = ?
            WHERE job_name = ?
            """;
        return jdbcTemplate.update(sql,
                TimeZoneUtils.toTimestamp(executionTime),
                TimeZoneUtils.toTimestamp(nextRun),
                jobName) > 0;
    }

    public boolean updateNextScheduledRun(String jobName, Instant nextRun) {
        String sql = "UPDATE job_tracker SET next_scheduled_run_date = ? WHERE job_name = ?";
        return jdbcTemplate.update(sql, TimeZoneUtils.toTimestamp(nextRun), jobName) > 0;
    }

    public boolean disableJob(String jobName) {
        String sql = "UPDATE job_tracker SET enabled = FALSE WHERE job_name = ?";
        return jdbcTemplate.update(sql, jobName) > 0;
    }

    public boolean enableJob(String jobName) {
        String sql = "UPDATE job_tracker SET enabled = TRUE WHERE job_name = ?";
        return jdbcTemplate.update(sql, jobName) > 0;
    }


    private static class JobTrackerRowMapper implements RowMapper<JobTracker> {
        @Override
        public JobTracker mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new JobTracker(
                    rs.getLong("id"),
                    rs.getString("job_name"),
                    JobType.valueOf(rs.getString("job_type")),
                    TimeZoneUtils.fromTimestamp(rs.getTimestamp("last_successful_run_date")),
                    TimeZoneUtils.fromTimestamp(rs.getTimestamp("next_scheduled_run_date")),
                    rs.getBoolean("enabled")
            );
        }
    }
}