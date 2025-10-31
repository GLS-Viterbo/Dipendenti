package it.gls.dipendenti.job.model;

import java.time.Instant;

public record JobTracker(
        Long id,
        String jobName,
        JobType jobType,
        Instant lastSuccessfulRunDate,
        Instant nextScheduledRunDate,
        boolean enabled
) {
    public enum JobType {
        DAILY,
        MONTHLY,
        ON_DEMAND
    }

    /**
     * Check if job is overdue (should have run but didn't)
     */
    public boolean isOverdue(Instant now) {
        return enabled && nextScheduledRunDate.isBefore(now);
    }

    /**
     * Check if job should run now
     */
    public boolean shouldRunNow(Instant now) {
        return enabled &&
                (nextScheduledRunDate.equals(now) || nextScheduledRunDate.isBefore(now));
    }
}