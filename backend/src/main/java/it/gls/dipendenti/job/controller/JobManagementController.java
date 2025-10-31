package it.gls.dipendenti.job.controller;

import it.gls.dipendenti.job.model.JobTracker;
import it.gls.dipendenti.job.repository.JobTrackerRepository;
import it.gls.dipendenti.job.MainJobScheduler;
import it.gls.dipendenti.job.service.JobOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobManagementController {

    private final MainJobScheduler scheduler;
    private final JobOrchestratorService orchestrator;
    private final JobTrackerRepository trackerRepository;

    public JobManagementController(MainJobScheduler scheduler,
                                   JobOrchestratorService orchestrator,
                                   JobTrackerRepository trackerRepository) {
        this.scheduler = scheduler;
        this.orchestrator = orchestrator;
        this.trackerRepository = trackerRepository;
    }

    /**
     * Get status of all jobs
     * GET /api/jobs
     */
    @GetMapping
    public ResponseEntity<List<JobTracker>> getAllJobs() {
        List<JobTracker> jobs = trackerRepository.findAllEnabled();
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get status of a specific job
     * GET /api/jobs/{jobName}
     */
    @GetMapping("/{jobName}")
    public ResponseEntity<JobTracker> getJobStatus(@PathVariable String jobName) {
        return trackerRepository.findByJobName(jobName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all overdue jobs
     * GET /api/jobs/overdue
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<JobTracker>> getOverdueJobs() {
        List<JobTracker> overdueJobs = orchestrator.findJobsToRun();
        return ResponseEntity.ok(overdueJobs);
    }

    /**
     * Manually trigger monthly accrual job
     * POST /api/jobs/monthly-accrual/trigger
     */
    @PostMapping("/monthly-accrual/trigger")
    public ResponseEntity<Map<String, String>> triggerMonthlyAccrual() {
        Map<String, String> response = new HashMap<>();

        try {
            scheduler.manualTriggerMonthlyAccrual();
            response.put("status", "success");
            response.put("message", "Monthly accrual job triggered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error triggering job: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger shift generation job
     * POST /api/jobs/shift-generation/trigger
     */
    @PostMapping("/shift-generation/trigger")
    public ResponseEntity<Map<String, String>> triggerShiftGeneration() {
        Map<String, String> response = new HashMap<>();

        try {
            scheduler.manualTriggerShiftGeneration();
            response.put("status", "success");
            response.put("message", "Shift generation job triggered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error triggering job: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger deadline notification job
     * POST /api/jobs/deadline-notifications/trigger
     */
    @PostMapping("/deadline-notifications/trigger")
    public ResponseEntity<Map<String, String>> triggerDeadlineNotifications() {
        Map<String, String> response = new HashMap<>();

        try {
            scheduler.manualTriggerDeadlineNotifications();
            response.put("status", "success");
            response.put("message", "Deadline notification job triggered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error triggering job: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Enable a disabled job
     * PUT /api/jobs/{jobName}/enable
     */
    @PutMapping("/{jobName}/enable")
    public ResponseEntity<Map<String, String>> enableJob(@PathVariable String jobName) {
        Map<String, String> response = new HashMap<>();

        boolean success = orchestrator.enableJob(jobName);

        if (success) {
            response.put("status", "success");
            response.put("message", "Job enabled successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "Failed to enable job - job not found");
            return ResponseEntity.notFound().build();
        }
    }
}
