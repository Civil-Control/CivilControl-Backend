package PSG.backEnd.exception.vehicle;

import PSG.backEnd.service.util.MessageSourceHelper;
import PSG.backEnd.config.ApplicationContextProvider;

public class ProjectAreaNotValidException extends RuntimeException {
    public ProjectAreaNotValidException(Long projectAreaId) {
        super(getMessage(projectAreaId));
    }

    private static String getMessage(Long projectAreaId) {
        try {
            MessageSourceHelper helper = ApplicationContextProvider.getApplicationContext()
                    .getBean(MessageSourceHelper.class);
            return helper.getMessage("vehicle.projectArea.notValid", projectAreaId);
        } catch (Exception e) {
            return "El area de proyecto con ID " + projectAreaId + " no existe o no esta disponible";
        }
    }
}
