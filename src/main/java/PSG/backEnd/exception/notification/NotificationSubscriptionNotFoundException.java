package PSG.backEnd.exception.notification;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.service.util.MessageSourceHelper;

public class NotificationSubscriptionNotFoundException extends NotFoundException {
    public NotificationSubscriptionNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("notification.subscription.notFound", id));
    }
}
