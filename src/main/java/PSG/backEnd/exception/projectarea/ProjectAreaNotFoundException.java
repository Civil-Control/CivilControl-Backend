package PSG.backEnd.exception.projectarea;

public class ProjectAreaNotFoundException extends RuntimeException {
    public ProjectAreaNotFoundException(Long id) {
        super("Project area with id " + id + " not found");
    }
}
