package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.notification.NotificationInboxItemDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionDTO;
import PSG.backEnd.model.dto.notification.NotificationSubscriptionResponseDTO;

import java.util.List;

public interface INotificationSubscriptionService {
    NotificationSubscriptionResponseDTO createSubscription(NotificationSubscriptionDTO dto);
    NotificationSubscriptionResponseDTO getSubscriptionById(Long id);
    NotificationSubscriptionResponseDTO updateSubscription(Long id, NotificationSubscriptionDTO dto);
    void deleteSubscription(Long id);
    List<NotificationSubscriptionResponseDTO> getMySubscriptions();
    List<NotificationSubscriptionResponseDTO> getUserSubscriptions(Long userId);
    List<NotificationInboxItemDTO> getInbox(int limit);
}
