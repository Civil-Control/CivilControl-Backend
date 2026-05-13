package PSG.backEnd.exception.notification;

public class NotificationSubscriptionNotValidException extends RuntimeException {
    public NotificationSubscriptionNotValidException(String message) {
        super(message);
    }
}
