package PSG.backEnd.model.dto;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.constraints.*;

public record AddressDTO(
    @NotBlank(message = "Street cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "Street must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    String street,

    @NotNull(message = "Street number cannot be null", groups = OnCreate.class)
    @Min(value = 1, message = "Street number must be positive", groups = {OnCreate.class, OnUpdate.class})
    @Max(value = 99999, message = "Street number is too large", groups = {OnCreate.class, OnUpdate.class})
    Integer number,

    @NotBlank(message = "City cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "City must contain only letters, spaces, dots, hyphens and apostrophes", groups = {OnCreate.class, OnUpdate.class})
    String city,

    @NotBlank(message = "State/Province cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "State/Province must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "State/Province must contain only letters, spaces, dots, hyphens and apostrophes", groups = {OnCreate.class, OnUpdate.class})
    String state,

    @NotBlank(message = "Country cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "Country must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "Country must contain only letters, spaces, dots, hyphens and apostrophes", groups = {OnCreate.class, OnUpdate.class})
    String country,

    @NotBlank(message = "Zip code cannot be blank", groups = OnCreate.class)
    @Pattern(regexp = "^[A-Z0-9]{4,8}$", message = "Zip code must be between 4 and 8 alphanumeric characters", groups = {OnCreate.class, OnUpdate.class})
    String zipCode
) {}
