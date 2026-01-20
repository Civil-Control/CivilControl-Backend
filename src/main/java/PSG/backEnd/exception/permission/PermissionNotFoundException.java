package PSG.backEnd.exception.permission;

import PSG.backEnd.service.util.MessageSourceHelper;

/**
 * Exception thrown when a requested permission is not found.
 */
public class PermissionNotFoundException extends RuntimeException {

    public PermissionNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("permissionNotFound.notFound", id));
    }

    public PermissionNotFoundException(String name) {
        super(MessageSourceHelper.getMessageStatic("permissionNotFound.notFound", name));
    }
}

