package it.gls.dipendenti.notification.controller;

import it.gls.dipendenti.hr.model.EmployeeDeadline;
import it.gls.dipendenti.hr.service.EmployeeDeadlineService;
import it.gls.dipendenti.notification.scheduler.DeadlineNotificationScheduler;
import it.gls.dipendenti.notification.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller per testare manualmente il sistema di notifiche
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationTestController {

    private final EmailService emailService;
    private final EmployeeDeadlineService deadlineService;
    private final DeadlineNotificationScheduler scheduler;

    public NotificationTestController(EmailService emailService,
                                      EmployeeDeadlineService deadlineService,
                                      DeadlineNotificationScheduler scheduler) {
        this.emailService = emailService;
        this.deadlineService = deadlineService;
        this.scheduler = scheduler;
    }

    /**
     * Testa l'invio di una notifica per una specifica scadenza
     * GET /api/notifications/test/{deadlineId}
     */
    @GetMapping("/test/{deadlineId}")
    public ResponseEntity<Map<String, String>> testSingleNotification(@PathVariable Long deadlineId) {
        Map<String, String> response = new HashMap<>();

        try {
            EmployeeDeadline deadline = deadlineService.getDeadlineById(deadlineId);
            emailService.sendDeadlineNotification(deadline);

            response.put("status", "success");
            response.put("message", "Email di test inviata con successo per la scadenza ID: " + deadlineId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Errore nell'invio dell'email: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Esegue manualmente il controllo delle scadenze
     * POST /api/notifications/check
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> manualCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<EmployeeDeadline> deadlines = deadlineService.getDeadlinesNeedingNotification();
            scheduler.manualNotificationCheck();

            response.put("status", "success");
            response.put("message", "Controllo manuale completato");
            response.put("deadlinesProcessed", deadlines.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Errore durante il controllo: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Ottiene la lista di scadenze che necessitano notifica
     * GET /api/notifications/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<EmployeeDeadline>> getPendingNotifications() {
        List<EmployeeDeadline> deadlines = deadlineService.getDeadlinesNeedingNotification();
        return ResponseEntity.ok(deadlines);
    }
}