package PSG.backEnd.exception.projectarea;

import PSG.backEnd.service.util.MessageSourceHelper;
import PSG.backEnd.config.ApplicationContextProvider;

public class ProjectAreaAlreadyExistsException extends RuntimeException {
    public ProjectAreaAlreadyExistsException(String name) {
        super(getMessage(name));
    }

    private static String getMessage(String name) {
        try {
            MessageSourceHelper helper = ApplicationContextProvider.getApplicationContext()
                    .getBean(MessageSourceHelper.class);
            return helper.getMessage("projectArea.alreadyExists", name);
        } catch (Exception e) {
            return "Ya existe un area de proyecto con el nombre '" + name + "' activa";
        }
    }
}
