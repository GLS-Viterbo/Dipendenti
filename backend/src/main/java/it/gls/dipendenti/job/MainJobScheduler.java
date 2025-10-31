package it.gls.dipendenti.job;

import it.gls.dipendenti.absence.service.AbsenceService;
import it.gls.dipendenti.hr.model.Company;
import it.gls.dipendenti.hr.model.EmployeeDeadline;
import it.gls.dipendenti.hr.repository.CompanyRepository;
import it.gls.dipendenti.hr.service.EmployeeDeadlineService;
import it.gls.dipendenti.job.model.JobTracker;
import it.gls.dipendenti.job.service.JobOrchestratorService;
import it.gls.dipendenti.job.service.JobOrchestratorService.JobExecutionResult;
import it.gls.dipendenti.notification.service.EmailService;
import it.gls.dipendenti.shift.service.ShiftAssignmentService;
import it.gls.dipendenti.util.TimeZoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class MainJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MainJobScheduler.class);

    private static final String JOB_MONTHLY_ACCRUAL = "monthly_accrual";
    private static final String JOB_SHIFT_GENERATION = "shift_generation";
    private static final String JOB_DEADLINE_NOTIFICATION = "deadline_notification";

    private final JobOrchestratorService orchestrator;
    private final AbsenceService absenceService;
    private final ShiftAssignmentService shiftAssignmentService;
    private final EmployeeDeadlineService deadlineService;
    private final EmailService emailService;
    private final CompanyRepository companyRepository;

    public MainJobScheduler(JobOrchestratorService orchestrator,
                            AbsenceService absenceService,
                            ShiftAssignmentService shiftAssignmentService,
                            EmployeeDeadlineService deadlineService,
                            EmailService emailService,
                            CompanyRepository companyRepository) {
        this.orchestrator = orchestrator;
        this.absenceService = absenceService;
        this.shiftAssignmentService = shiftAssignmentService;
        this.deadlineService = deadlineService;
        this.emailService = emailService;
        this.companyRepository = companyRepository;
    }

    // ===================================================================
    // RECOVERY AT STARTUP
    // ===================================================================

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStartup() {
        logger.info("═══════════════════════════════════════════════════");
        logger.info("  APPLICATION STARTED - Running Job Recovery Check");
        logger.info("═══════════════════════════════════════════════════");

        try {
            checkAndRunMissedJobs();
        } catch (Exception e) {
            logger.error("Critical error during startup job recovery", e);
        }

        logger.info("═══════════════════════════════════════════════════");
        logger.info("  Job Recovery Check Completed");
        logger.info("═══════════════════════════════════════════════════");
    }

    // ===================================================================
    // PERIODIC CHECK FOR MISSED JOBS (Every hour)
    // ===================================================================

    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    public void hourlyJobCheck() {
        logger.info("Running hourly job check...");
        checkAndRunMissedJobs();
    }

    // ===================================================================
    // SCHEDULED JOBS
    // ===================================================================

    /**
     * Daily shift generation at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledShiftGeneration() {
        logger.info("Scheduled shift generation triggered");
        runShiftGenerationJob();
    }

    /**
     * Daily deadline notifications at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledDeadlineNotifications() {
        logger.info("Scheduled deadline notifications triggered");
        runDeadlineNotificationJob();
    }

    /**
     * Monthly accrual at 1:00 AM on the 1st of each month
     */
    @Scheduled(cron = "0 0 1 1 * *")
    public void scheduledMonthlyAccrual() {
        logger.info("Scheduled monthly accrual triggered");
        runMonthlyAccrualJob();
    }

    // ===================================================================
    // CORE JOB RECOVERY LOGIC
    // ===================================================================

    private void checkAndRunMissedJobs() {
        List<JobTracker> overdueJobs = orchestrator.findJobsToRun();

        if (overdueJobs.isEmpty()) {
            logger.info("No missed jobs found - all jobs are up to date");
            return;
        }

        logger.warn("Found {} overdue job(s) that need recovery", overdueJobs.size());

        for (JobTracker job : overdueJobs) {
            logger.info("Processing overdue job: {} (last run: {}, next scheduled: {})",
                    job.jobName(),
                    job.lastSuccessfulRunDate(),
                    job.nextScheduledRunDate());

            switch (job.jobName()) {
                case JOB_MONTHLY_ACCRUAL -> runMonthlyAccrualWithCatchup(job);
                case JOB_SHIFT_GENERATION -> runShiftGenerationJob();
                case JOB_DEADLINE_NOTIFICATION -> runDeadlineNotificationJob();
                default -> logger.warn("Unknown job type: {}", job.jobName());
            }
        }
    }

    // ===================================================================
    // JOB 1: MONTHLY ACCRUAL (with catchup logic)
    // ===================================================================

    private void runMonthlyAccrualJob() {
        orchestrator.executeJob(JOB_MONTHLY_ACCRUAL, () -> {
            try {
                absenceService.monthlyAccrualJob();

                Instant nextRun = orchestrator.calculateNextMonthlyRun(Instant.now());

                return JobExecutionResult.success(
                        "Monthly accrual completed successfully",
                        1,
                        nextRun
                );
            } catch (Exception e) {
                logger.error("Error in monthly accrual job", e);
                return JobExecutionResult.failed(e.getMessage());
            }
        });
    }

    /**
     * Run monthly accrual with catchup for missed months
     */
    private void runMonthlyAccrualWithCatchup(JobTracker tracker) {
        orchestrator.executeJob(JOB_MONTHLY_ACCRUAL, () -> {
            try {
                Instant now = Instant.now();
                Instant lastRun = tracker.lastSuccessfulRunDate();
                Instant nextScheduled = tracker.nextScheduledRunDate();

                if (lastRun == null) {
                    // First run ever
                    logger.info("First time running monthly accrual - executing once");
                    absenceService.monthlyAccrualJob();

                    Instant nextRun = orchestrator.calculateNextMonthlyRun(now);
                    return JobExecutionResult.success(
                            "First monthly accrual completed", 1, nextRun
                    );
                }

                // Calculate how many months were missed
                LocalDate lastRunDate = TimeZoneUtils.toCompanyDate(lastRun);
                LocalDate today = TimeZoneUtils.todayCompanyDate();

                int missedMonths = 0;
                LocalDate cursor = lastRunDate.withDayOfMonth(1).plusMonths(1);

                while (cursor.isBefore(today) || cursor.isEqual(today.withDayOfMonth(1))) {
                    missedMonths++;
                    cursor = cursor.plusMonths(1);
                }

                logger.info("Detected {} missed month(s) for accrual - running catchup", missedMonths);

                // Run accrual for each missed month
                for (int i = 0; i < missedMonths; i++) {
                    logger.info("Running accrual #{}/{}", i + 1, missedMonths);
                    absenceService.monthlyAccrualJob();
                }

                Instant nextRun = orchestrator.calculateNextMonthlyRun(now);

                return JobExecutionResult.success(
                        String.format("Catchup completed: %d month(s) processed", missedMonths),
                        missedMonths,
                        nextRun
                );

            } catch (Exception e) {
                logger.error("Error in monthly accrual catchup", e);
                return JobExecutionResult.failed(e.getMessage());
            }
        });
    }

    // ===================================================================
    // JOB 2: SHIFT GENERATION
    // ===================================================================

    private void runShiftGenerationJob() {
        orchestrator.executeJob(JOB_SHIFT_GENERATION, () -> {
            try {
                LocalDate today = TimeZoneUtils.todayCompanyDate();
                LocalDate startDate = today.plusDays(1);
                LocalDate endDate = startDate.plusDays(14);

                logger.info("Generating shifts from {} to {}", startDate, endDate);

                List<Company> companies = companyRepository.findAll();
                int totalGenerated = 0;
                int successfulCompanies = 0;
                int failedCompanies = 0;

                for (Company company : companies) {
                    try {
                        logger.info("Generating shifts for company: {} (ID: {})",
                                company.name(), company.id());

                        int generated = shiftAssignmentService.generateAssignmentsForDateRange(
                                startDate, endDate, company.id()
                        );

                        totalGenerated += generated;
                        successfulCompanies++;

                        logger.info("Successfully generated {} shifts for company: {}",
                                generated, company.name());

                    } catch (Exception e) {
                        failedCompanies++;
                        logger.error("Error generating shifts for company: {} (ID: {})",
                                company.name(), company.id(), e);
                        // Continua con la prossima azienda invece di fallire tutto
                    }
                }

                // Calcola il prossimo run DOPO aver processato tutte le aziende
                Instant nextRun = orchestrator.calculateNextDailyRun(Instant.now());

                // Crea un messaggio di summary
                String message = String.format(
                        "Generated %d shifts for period %s to %s. " +
                                "Companies processed: %d successful, %d failed out of %d total",
                        totalGenerated, startDate, endDate,
                        successfulCompanies, failedCompanies, companies.size()
                );

                if (failedCompanies > 0 && successfulCompanies == 0) {
                    // Se tutte le aziende falliscono, considera il job fallito
                    return JobExecutionResult.failed(message);
                }

                return JobExecutionResult.success(message, totalGenerated, nextRun);

            } catch (Exception e) {
                logger.error("Critical error in shift generation job", e);
                return JobExecutionResult.failed("Critical error: " + e.getMessage());
            }
        });
    }

    // ===================================================================
    // JOB 3: DEADLINE NOTIFICATIONS
    // ===================================================================

    private void runDeadlineNotificationJob() {
        orchestrator.executeJob(JOB_DEADLINE_NOTIFICATION, () -> {
            try {
                List<EmployeeDeadline> deadlines = deadlineService.getDeadlinesNeedingNotification();

                logger.info("Found {} deadline(s) requiring notification", deadlines.size());

                int successCount = 0;
                int failureCount = 0;

                for (EmployeeDeadline deadline : deadlines) {
                    try {
                        emailService.sendDeadlineNotification(deadline);
                        deadlineService.markDeadlineAsNotified(deadline.id());
                        successCount++;

                        logger.debug("Notification sent for deadline ID: {}", deadline.id());
                    } catch (Exception e) {
                        failureCount++;
                        logger.error("Failed to send notification for deadline ID: {}",
                                deadline.id(), e);
                    }
                }

                // Calculate next run (tomorrow at 9 AM)
                LocalDate tomorrow = TimeZoneUtils.todayCompanyDate().plusDays(1);
                Instant nextRun = TimeZoneUtils.combine(tomorrow, java.time.LocalTime.of(9, 0));

                String details = String.format(
                        "Processed %d deadline(s): %d successful, %d failed",
                        deadlines.size(), successCount, failureCount
                );

                if (failureCount > 0) {
                    logger.warn("Some notifications failed: {} failures out of {} total",
                            failureCount, deadlines.size());
                }

                return JobExecutionResult.success(details, successCount, nextRun);

            } catch (Exception e) {
                logger.error("Error in deadline notification job", e);
                return JobExecutionResult.failed(e.getMessage());
            }
        });
    }

    // ===================================================================
    // MANUAL TRIGGER METHODS (for testing/admin)
    // ===================================================================

    public void manualTriggerMonthlyAccrual() {
        logger.info("Manual trigger: Monthly Accrual");
        runMonthlyAccrualJob();
    }

    public void manualTriggerShiftGeneration() {
        logger.info("Manual trigger: Shift Generation");
        runShiftGenerationJob();
    }

    public void manualTriggerDeadlineNotifications() {
        logger.info("Manual trigger: Deadline Notifications");
        runDeadlineNotificationJob();
    }
}