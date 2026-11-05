package PSG.backEnd.service.notification.resolver;

import PSG.backEnd.model.entity.notification.SubjectDueDateInfo;
import PSG.backEnd.model.enums.notification.NotificationSubjectType;

import java.util.List;

public interface NextDueDateResolver {

    NotificationSubjectType getSubjectType();

    List<SubjectDueDateInfo> resolveForId(Long subjectId);

    List<SubjectDueDateInfo> resolveAll();
}
