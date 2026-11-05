package PSG.backEnd.exception.user;

import PSG.backEnd.service.util.MessageSourceHelper;

/**
 * Exception thrown when a requested user is not found.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("user.notFound", id));
    }

    public UserNotFoundException(String username) {
        super(MessageSourceHelper.getMessageStatic("user.notFoundByUsername", username));
    }
}
