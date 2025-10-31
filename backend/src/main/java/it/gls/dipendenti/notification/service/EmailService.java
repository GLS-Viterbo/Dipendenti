package it.gls.dipendenti.notification.service;

import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.model.EmployeeDeadline;
import it.gls.dipendenti.hr.service.EmployeeService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmployeeService employeeService;

    @Value("${notification.email.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        EmployeeService employeeService) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.employeeService = employeeService;
    }

    /**
     * Invia una notifica email per una scadenza
     * @param deadline la scadenza da notificare
     */
    public void sendDeadlineNotification(EmployeeDeadline deadline) {
        try {
            Employee employee = employeeService.getEmployeeById(deadline.employeeId());

            // Determina il destinatario
            String recipient = deadline.recipientEmail() != null && !deadline.recipientEmail().isBlank()
                    ? deadline.recipientEmail()
                    : null;

            if (recipient == null || recipient.isBlank()) {
                logger.warn("Nessun destinatario valido per la scadenza ID: {}", deadline.id());
                return;
            }

            // Prepara il contenuto HTML
            String htmlContent = buildDeadlineEmailContent(employee, deadline);

            // Crea e invia il messaggio
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient);
            helper.setSubject(buildSubject(deadline));
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail); // Usa il mittente configurato

            mailSender.send(message);

            logger.info("Email di notifica inviata con successo per la scadenza ID: {} a {}",
                    deadline.id(), recipient);

        } catch (MessagingException e) {
            logger.error("Errore nell'invio dell'email per la scadenza ID: {}", deadline.id(), e);
            throw new EmailSendException("Impossibile inviare l'email di notifica", e);
        } catch (Exception e) {
            logger.error("Errore imprevisto durante l'invio dell'email per la scadenza ID: {}",
                    deadline.id(), e);
            throw new EmailSendException("Errore imprevisto nell'invio dell'email", e);
        }
    }

    /**
     * Costruisce il contenuto HTML dell'email utilizzando il template Thymeleaf
     */
    private String buildDeadlineEmailContent(Employee employee, EmployeeDeadline deadline) {
        Context context = new Context(Locale.ITALY);

        // Informazioni dipendente
        String fullName = employee.name() + " " + employee.surname();
        String initials = getInitials(employee.name(), employee.surname());

        context.setVariable("employeeName", fullName);
        context.setVariable("employeeInitials", initials);

        // Informazioni scadenza
        context.setVariable("deadlineType", formatDeadlineType(deadline.type()));
        context.setVariable("expirationDate", deadline.expirationDate());

        // Calcola i giorni rimanenti
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, deadline.expirationDate());
        boolean isExpired = daysRemaining < 0;

        context.setVariable("daysRemaining", Math.abs(daysRemaining));
        context.setVariable("isExpired", isExpired);

        // Note (se presenti)
        if (deadline.note() != null && !deadline.note().isBlank()) {
            context.setVariable("note", deadline.note());
        }

        // Processa il template
        return templateEngine.process("deadline-notification", context);
    }

    /**
     * Costruisce l'oggetto dell'email
     */
    private String buildSubject(EmployeeDeadline deadline) {
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, deadline.expirationDate());

        String deadlineTypeLabel = formatDeadlineType(deadline.type());

        if (daysRemaining < 0) {
            return String.format("🔴 SCADENZA SUPERATA: %s", deadlineTypeLabel);
        } else if (daysRemaining == 0) {
            return String.format("⚠️ SCADENZA OGGI: %s", deadlineTypeLabel);
        } else if (daysRemaining <= 7) {
            return String.format("⚠️ SCADENZA URGENTE: %s (tra %d giorni)",
                    deadlineTypeLabel, daysRemaining);
        } else {
            return String.format("📅 Promemoria Scadenza: %s (tra %d giorni)",
                    deadlineTypeLabel, daysRemaining);
        }
    }

    /**
     * Estrae le iniziali dal nome e cognome
     */
    private String getInitials(String name, String surname) {
        String nameInitial = name != null && !name.isEmpty()
                ? name.substring(0, 1).toUpperCase()
                : "";
        String surnameInitial = surname != null && !surname.isEmpty()
                ? surname.substring(0, 1).toUpperCase()
                : "";
        return nameInitial + surnameInitial;
    }

    /**
     * Formatta il tipo di scadenza in italiano
     */
    private String formatDeadlineType(String type) {
        return switch (type) {
            case "CONTRATTO" -> "Contratto";
            case "CERTIFICATO_MEDICO" -> "Certificato Medico";
            case "FORMAZIONE" -> "Formazione";
            case "DOCUMENTO" -> "Documento";
            case "ALTRO" -> "Altro";
            default -> type;
        };
    }

    /**
     * Eccezione personalizzata per errori di invio email
     */
    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}