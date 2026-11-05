package PSG.backEnd.model.dto.address;

public record AddressResponseDTO(

    String street,

    Integer number,

    String city,

    String state,

    String country,

    String zipCode
) {}
