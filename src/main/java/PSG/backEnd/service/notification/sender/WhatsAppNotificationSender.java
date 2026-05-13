package PSG.backEnd.service.notification.sender;

import PSG.backEnd.model.entity.notification.NotificationPayload;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationSender implements NotificationSender {

    private final RestClient whatsAppRestClient;

    @Value("${whatsapp.cloud-api.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.cloud-api.access-token:}")
    private String accessToken;

    private static final Map<NotificationSubjectType, String> TEMPLATE_NAMES = Map.of(
            NotificationSubjectType.VEHICLE_VTV,        "vtv_expiration_alert",
            NotificationSubjectType.CHECK_PAYMENT,      "check_payment_alert",
            NotificationSubjectType.INSURANCE_POLICY,   "insurance_policy_alert",
            NotificationSubjectType.SERVICE_ASSIGNMENT, "service_assignment_alert",
            NotificationSubjectType.WORK_CONTRACT,      "work_contract_alert"
    );

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.WHATSAPP; }

    @Override
    public void send(NotificationPayload payload) {
        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("WhatsApp notification skipped: WHATSAPP_ACCESS_TOKEN or WHATSAPP_PHONE_ID not configured");
            return;
        }
        String phoneNumber = payload.channelAddresses().get(NotificationChannel.WHATSAPP);
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.warn("WhatsApp notification skipped for userId={}: no phone number", payload.userId());
            return;
        }

        String templateName   = TEMPLATE_NAMES.get(payload.subjectType());
        String dueDateFormatted = payload.dueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        Map<String, Object> requestBody = Map.of(
                "messaging_product", "whatsapp",
                "to", phoneNumber,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", "es_AR"),
                        "components", List.of(
                                Map.of(
                                        "type", "body",
                                        "parameters", List.of(
                                                Map.of("type", "text", "text", payload.subjectDisplayName()),
                                                Map.of("type", "text", "text", dueDateFormatted),
                                                Map.of("type", "text", "text", String.valueOf(payload.daysUntilDue()))
                                        )
                                )
                        )
                )
        );

        whatsAppRestClient.post()
                .uri("/{phoneNumberId}/messages", phoneNumberId)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }
}
