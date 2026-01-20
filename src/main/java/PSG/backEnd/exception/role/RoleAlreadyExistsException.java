package PSG.backEnd.exception.role;

import PSG.backEnd.service.util.MessageSourceHelper;

/**
 * Exception thrown when attempting to create a role that already exists.
 */
public class RoleAlreadyExistsException extends RuntimeException {
    
    public RoleAlreadyExistsException(String name) {
        super(MessageSourceHelper.getMessageStatic("role.name.alreadyExists", name));
    }
}

