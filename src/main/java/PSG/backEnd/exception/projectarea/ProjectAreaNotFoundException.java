package PSG.backEnd.exception.projectarea;

import PSG.backEnd.service.util.MessageSourceHelper;

public class ProjectAreaNotFoundException extends RuntimeException {
    public ProjectAreaNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("projectAreaNotFound.notFound", id));
    }
}
