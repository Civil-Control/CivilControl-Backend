package PSG.backEnd.exception.role;

import PSG.backEnd.service.util.MessageSourceHelper;

/**
 * Exception thrown when a requested role is not found.
 */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("roleNotFound.notFound", id));
    }

    public RoleNotFoundException(String name) {
        super(MessageSourceHelper.getMessageStatic("roleNotFound.notFound", name));
    }
}
