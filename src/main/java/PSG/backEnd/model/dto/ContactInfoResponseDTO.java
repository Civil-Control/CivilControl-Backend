package PSG.backEnd.model.dto;

import java.util.List;

public record ContactInfoResponseDTO(

    List<String> email,

    List<String> phoneNumber
) {}
