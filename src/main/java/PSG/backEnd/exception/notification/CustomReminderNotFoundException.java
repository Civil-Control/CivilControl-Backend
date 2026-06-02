package PSG.backEnd.exception.notification;

import PSG.backEnd.exception.NotFoundException;
import PSG.backEnd.service.util.MessageSourceHelper;

public class CustomReminderNotFoundException extends NotFoundException {
    public CustomReminderNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("custom.reminder.notFound", id));
    }
}
