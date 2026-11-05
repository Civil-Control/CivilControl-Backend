package PSG.backEnd.service.notification.sender;

import PSG.backEnd.model.entity.notification.NotificationPayload;
import PSG.backEnd.model.enums.notification.NotificationChannel;

public interface NotificationSender {
    NotificationChannel getChannel();
    void send(NotificationPayload payload);
}
