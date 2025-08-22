package PSG.backEnd.model.dto.contactInfo;

import java.util.List;

public record ContactInfoResponseDTO(

    List<String> email,

    List<String> phoneNumber
) {}
