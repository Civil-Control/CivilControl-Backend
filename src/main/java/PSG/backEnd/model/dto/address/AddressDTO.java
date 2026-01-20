package PSG.backEnd.model.dto.address;

import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Data Transfer Object for physical addresses. " +
        "Represents a complete mailing address including street, number, city, state/province, country, and postal code.")
public record AddressDTO(

    @Schema(description = "Street or avenue name. Must contain only valid address characters. " +
            "Minimum 2 characters, maximum 200 characters.",
            example = "Av. Colón",
            minLength = 2,
            maxLength = 200,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 2, max = 200, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String street,

    @Schema(description = "Street number or building number. Must be a positive integer between 1 and 99999.",
            example = "1234",
            minimum = "1",
            maximum = "99999",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.required}", groups = OnCreate.class)
    @Min(value = 1, message = "{validation.positive}", groups = {OnCreate.class, OnUpdate.class})
    @Max(value = 99999, message = "Street number is too large", groups = {OnCreate.class, OnUpdate.class})
    Integer number,

    @Schema(description = "City or municipality name. Must contain only letters, spaces, dots, hyphens and apostrophes. " +
            "Minimum 2 characters, maximum 100 characters.",
            example = "Córdoba",
            minLength = 2,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
    String city,

    @Schema(description = "State or province name. Must contain only letters, spaces, dots, hyphens and apostrophes. " +
            "Minimum 2 characters, maximum 100 characters.",
            example = "Córdoba",
            minLength = 2,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
    String state,

    @Schema(description = "Country name. Must contain only letters, spaces, dots, hyphens and apostrophes. " +
            "Minimum 2 characters, maximum 100 characters.",
            example = "Argentina",
            minLength = 2,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
    String country,

    @Schema(description = "Postal code or ZIP code. Must be between 4 and 20 alphanumeric characters in uppercase. " +
            "Format varies by country (e.g., 5000 for Argentina, or X5000ABC).",
            example = "5000",
            pattern = "^[A-Z0-9]{4,20}$",
            minLength = 4,
            maxLength = 20,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Pattern(regexp = "^[A-Z0-9]{4,20}$", message = "{validation.size}", groups = {OnCreate.class, OnUpdate.class})
    String zipCode
) {}
