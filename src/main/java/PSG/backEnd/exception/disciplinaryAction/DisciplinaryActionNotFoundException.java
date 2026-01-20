package PSG.backEnd.exception.disciplinaryAction;

import PSG.backEnd.service.util.MessageSourceHelper;

public class DisciplinaryActionNotFoundException extends RuntimeException {
    public DisciplinaryActionNotFoundException(Long id) {
        super(MessageSourceHelper.getMessageStatic("disciplinaryActionNotFound.notFound", id));
    }
}

