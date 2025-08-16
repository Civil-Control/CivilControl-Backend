package PSG.backEnd.model.dto;

import jakarta.validation.constraints.*;

public record AddressResponseDTO(

    String street,

    Integer number,

    String city,

    String state,

    String country,

    String zipCode
) {}
