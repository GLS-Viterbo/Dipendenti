package it.gls.dipendenti.notification.scheduler;

import it.gls.dipendenti.hr.model.EmployeeDeadline;
import it.gls.dipendenti.hr.service.EmployeeDeadlineService;
import it.gls.dipendenti.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DeadlineNotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DeadlineNotificationScheduler.class);

    private final EmployeeDeadlineService deadlineService;
    private final EmailService emailService;

    public DeadlineNotificationScheduler(EmployeeDeadlineService deadlineService,
                                         EmailService emailService) {
        this.deadlineService = deadlineService;
        this.emailService = emailService;
    }

    /**
     * Job schedulato che controlla e notifica le scadenze
     * Esegue ogni giorno alle 9:00
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void checkAndNotifyDeadlines() {
        logger.info("Avvio controllo scadenze per notifiche...");

        try {
            // Ottieni tutte le scadenze che necessitano notifica
            List<EmployeeDeadline> deadlinesNeedingNotification =
                    deadlineService.getDeadlinesNeedingNotification();

            logger.info("Trovate {} scadenze da notificare", deadlinesNeedingNotification.size());

            int successCount = 0;
            int failureCount = 0;

            for (EmployeeDeadline deadline : deadlinesNeedingNotification) {
                try {
                    // Invia la notifica email
                    emailService.sendDeadlineNotification(deadline);

                    // Marca la scadenza come notificata
                    deadlineService.markDeadlineAsNotified(deadline.id());

                    successCount++;
                    logger.debug("Notifica inviata con successo per scadenza ID: {}", deadline.id());

                } catch (Exception e) {
                    failureCount++;
                    logger.error("Errore nell'invio della notifica per scadenza ID: {}",
                            deadline.id(), e);
                }
            }

            logger.info("Controllo scadenze completato. Successi: {}, Fallimenti: {}",
                    successCount, failureCount);

        } catch (Exception e) {
            logger.error("Errore critico durante il controllo delle scadenze", e);
        }
    }

    /**
     * Job di verifica manuale (può essere chiamato via REST API per test)
     * Utile per testare il sistema senza aspettare lo scheduler
     */
    public void manualNotificationCheck() {
        logger.info("Esecuzione manuale controllo scadenze richiesta");
        checkAndNotifyDeadlines();
    }
}