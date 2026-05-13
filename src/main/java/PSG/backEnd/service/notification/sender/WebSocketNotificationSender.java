package PSG.backEnd.service.notification.sender;

import PSG.backEnd.model.entity.notification.NotificationPayload;
import PSG.backEnd.model.enums.notification.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketNotificationSender implements NotificationSender {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public NotificationChannel getChannel() { return NotificationChannel.SYSTEM; }

    @Override
    public void send(NotificationPayload payload) {
        messagingTemplate.convertAndSendToUser(
                payload.userId().toString(),
                "/queue/notifications",
                payload
        );
    }
}
