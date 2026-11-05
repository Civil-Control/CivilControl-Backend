package PSG.backEnd.service.notification.sender;

import PSG.backEnd.model.entity.notification.NotificationPayload;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final Resend resendClient;

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from-address}")
    private String fromAddress;

    @Value("${resend.from-name}")
    private String fromName;

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.EMAIL; }

    @Override
    public void send(NotificationPayload payload) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Email notification skipped: RESEND_API_KEY not configured");
            return;
        }
        String email = payload.channelAddresses().get(NotificationChannel.EMAIL);
        if (email == null || email.isBlank()) {
            log.warn("Email notification skipped for userId={}: no email address", payload.userId());
            return;
        }

        boolean isReminder = payload.subjectType() == NotificationSubjectType.CUSTOM_REMINDER;
        String subject = isReminder
                ? "CivilControl — Recordatorio: " + payload.subjectDisplayName()
                : "CivilControl — " + payload.subjectDisplayName() + " vence en " + payload.daysUntilDue() + " días";

        CreateEmailOptions request = CreateEmailOptions.builder()
                .from(fromName + " <" + fromAddress + ">")
                .to(email)
                .subject(subject)
                .html(buildHtmlBody(payload))
                .build();

        try {
            resendClient.emails().send(request);
        } catch (ResendException e) {
            throw new RuntimeException("Resend API error: " + e.getMessage(), e);
        }
    }

    private String buildHtmlBody(NotificationPayload p) {
        if (p.subjectType() == NotificationSubjectType.CUSTOM_REMINDER) {
            return buildReminderHtmlBody(p);
        }
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto">
                  <h2 style="color:#1a56db">CivilControl — Aviso de vencimiento</h2>
                  <p><strong>%s</strong></p>
                  <p>Fecha de vencimiento: <strong>%s</strong></p>
                  <p>Días restantes: <strong>%d</strong></p>
                  <hr/>
                  <small style="color:#6b7280">Este mensaje fue generado automáticamente. No responder.</small>
                </div>
                """.formatted(
                p.subjectDisplayName(),
                p.dueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                p.daysUntilDue());
    }

    private String buildReminderHtmlBody(NotificationPayload p) {
        String descBlock = (p.subjectDescription() != null && !p.subjectDescription().isBlank())
                ? "<p style=\"color:#374151\">" + p.subjectDescription() + "</p>"
                : "";
        String dayText = p.daysUntilDue() == 0
                ? "hoy"
                : "en " + p.daysUntilDue() + " día(s)";
        return """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto">
                  <h2 style="color:#1a56db">CivilControl — Recordatorio</h2>
                  <p><strong>%s</strong></p>
                  %s
                  <p>Fecha: <strong>%s</strong> (%s)</p>
                  <hr/>
                  <small style="color:#6b7280">Este mensaje fue generado automáticamente. No responder.</small>
                </div>
                """.formatted(
                p.subjectDisplayName(),
                descBlock,
                p.dueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                dayText);
    }
}
