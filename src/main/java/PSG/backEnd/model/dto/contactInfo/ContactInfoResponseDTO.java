package PSG.backEnd.model.dto.contactInfo;

import java.util.List;

public record ContactInfoResponseDTO(

    Long id,

    String referenceName,

    List<String> email,

    List<String> phoneNumber
) {}
