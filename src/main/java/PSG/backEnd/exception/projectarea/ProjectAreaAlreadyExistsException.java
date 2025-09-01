package PSG.backEnd.exception.projectarea;

public class ProjectAreaAlreadyExistsException extends RuntimeException {
    public ProjectAreaAlreadyExistsException(String name) {
        super("Project area with name '" + name + "' already exists and is active");
    }
}
