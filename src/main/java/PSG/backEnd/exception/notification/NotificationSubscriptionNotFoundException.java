package PSG.backEnd.exception.notification;

import PSG.backEnd.service.util.MessageSourceHelper;

public class NotificationSubscriptionNotFoundException extends RuntimeException {
    public NotificationSubscriptionNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("notification.subscription.notFound", id));
    }
}
