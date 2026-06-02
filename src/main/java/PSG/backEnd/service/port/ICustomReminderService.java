package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.notification.CustomReminderDTO;
import PSG.backEnd.model.dto.notification.CustomReminderResponseDTO;

import java.util.List;

public interface ICustomReminderService {

    CustomReminderResponseDTO createReminder(CustomReminderDTO dto);

    CustomReminderResponseDTO getReminderById(Long id);

    CustomReminderResponseDTO updateReminder(Long id, CustomReminderDTO dto);

    void deleteReminder(Long id);

    List<CustomReminderResponseDTO> getMyReminders();

    List<CustomReminderResponseDTO> getAllReminders();

    List<CustomReminderResponseDTO> getUserReminders(Long userId);
}
