package it.gls.dipendenti.job.service;

import it.gls.dipendenti.job.model.JobTracker;
import it.gls.dipendenti.job.repository.JobTrackerRepository;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class JobOrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(JobOrchestratorService.class);
    private static final int MAX_CONSECUTIVE_FAILURES = 5;

    private final JobTrackerRepository trackerRepository;

    public JobOrchestratorService(JobTrackerRepository trackerRepository) {
        this.trackerRepository = trackerRepository;
    }

    /**
     * Execute a job with full error handling, logging, and state management
     */
    @Transactional
    public JobExecutionResult executeJob(String jobName, Supplier<JobExecutionResult> jobLogic) {
        Instant startTime = Instant.now();

        logger.info("[JOB START] {} - Starting execution", jobName);

        // Validate job exists and is enabled
        Optional<JobTracker> trackerOpt = trackerRepository.findByJobName(jobName);
        if (trackerOpt.isEmpty()) {
            logger.error("[JOB ERROR] {} - Job not found in tracker", jobName);
            return JobExecutionResult.failed("Job not found in tracker");
        }

        JobTracker tracker = trackerOpt.get();

        if (!tracker.enabled()) {
            logger.warn("[JOB SKIPPED] {} - Job is disabled", jobName);
            return JobExecutionResult.skipped("Job is disabled");
        }

        // Execute job logic
        JobExecutionResult result;
        try {
            result = jobLogic.get();
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            if (result.isSuccess()) {
                logger.info("[JOB SUCCESS] {} - Completed in {}ms, processed {} records",
                        jobName, duration.toMillis(), result.recordsProcessed());

                // Update tracker
                trackerRepository.updateAfterSuccess(jobName, endTime, result.nextScheduledRun());


            } else {
                logger.error("[JOB FAILED] {} - Error: {}", jobName, result.errorMessage());
            }

        } catch (Exception e) {
            logger.error("[JOB EXCEPTION] {} - Unexpected error", jobName, e);

            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            result = JobExecutionResult.failed(errorMsg);
        }

        return result;
    }

    /**
     * Find all jobs that should run now (including overdue ones)
     */
    public List<JobTracker> findJobsToRun() {
        Instant now = TimeZoneUtils.nowInstant();
        return trackerRepository.findOverdueJobs(now);
    }

    /**
     * Calculate next run time for monthly job
     */
    public Instant calculateNextMonthlyRun(Instant lastRun) {
        LocalDate lastRunDate = TimeZoneUtils.toCompanyDate(lastRun);
        YearMonth lastMonth = YearMonth.from(lastRunDate);
        YearMonth nextMonth = lastMonth.plusMonths(1);

        // First day of next month
        LocalDate nextRunDate = nextMonth.atDay(1);
        return TimeZoneUtils.startOfDay(nextRunDate);
    }

    /**
     * Calculate next run time for daily job
     */
    public Instant calculateNextDailyRun(Instant lastRun) {
        LocalDate lastRunDate = TimeZoneUtils.toCompanyDate(lastRun);
        LocalDate nextRunDate = lastRunDate.plusDays(1);
        return TimeZoneUtils.startOfDay(nextRunDate);
    }

    /**
     * Enable a disabled job
     */
    @Transactional
    public boolean enableJob(String jobName) {
        logger.info("[JOB ENABLE] {} - Enabling job", jobName);
        return trackerRepository.enableJob(jobName);
    }

    /**
     * Result object for job execution
     */
    public static class JobExecutionResult {
        private final boolean success;
        private final String details;
        private final String errorMessage;
        private final int recordsProcessed;
        private final Instant nextScheduledRun;

        private JobExecutionResult(boolean success, String details, String errorMessage,
                                   int recordsProcessed, Instant nextScheduledRun) {
            this.success = success;
            this.details = details;
            this.errorMessage = errorMessage;
            this.recordsProcessed = recordsProcessed;
            this.nextScheduledRun = nextScheduledRun;
        }

        public static JobExecutionResult success(String details, int recordsProcessed, Instant nextRun) {
            return new JobExecutionResult(true, details, null, recordsProcessed, nextRun);
        }

        public static JobExecutionResult failed(String errorMessage) {
            return new JobExecutionResult(false, null, errorMessage, 0, null);
        }

        public static JobExecutionResult skipped(String reason) {
            return new JobExecutionResult(false, reason, null, 0, null);
        }

        public boolean isSuccess() { return success; }
        public String details() { return details; }
        public String errorMessage() { return errorMessage; }
        public int recordsProcessed() { return recordsProcessed; }
        public Instant nextScheduledRun() { return nextScheduledRun; }
    }
}