package PSG.backEnd.model.dto.supplier;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.dto.contactInfo.ContactInfoDTO;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record SupplierDTO(
    @NotBlank(message = "CUIT cannot be blank", groups = OnCreate.class)
    @Pattern(regexp = "^\\d{2}-\\d{8}-\\d$", message = "CUIT must have XX-XXXXXXXX-X format", groups = {OnCreate.class, OnUpdate.class})
    String cuit,

    @NotBlank(message = "Legal name cannot be blank", groups = OnCreate.class)
    @Size(min = 1, max = 50, message = "Legal name must be between 1 and 50 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^\\s*\\S.*$", message = "Legal name cannot be blank or only spaces", groups = {OnCreate.class, OnUpdate.class})
    String legalName,

    @NotBlank(message = "Trade name cannot be blank", groups = OnCreate.class)
    @Size(min = 1, max = 50, message = "Trade name must be between 1 and 50 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^\\s*\\S.*$", message = "Trade name cannot be blank or only spaces", groups = {OnCreate.class, OnUpdate.class})
    String tradeName,

    @NotEmpty(message = "At least one payment method must be specified", groups = OnCreate.class)
    List<@NotNull(message = "Payment method cannot be null", groups = {OnCreate.class, OnUpdate.class}) PaymentMethod> allowedPaymentMethods,

    @NotNull(message = "Address cannot be null", groups = OnCreate.class)
    @Valid
    AddressDTO address,

    @Valid
    ContactInfoDTO contactInfo,

    @NotNull(message = "Default discount percentage cannot be null", groups = OnCreate.class)
    @DecimalMin(value = "0.0", inclusive = true, message = "Default discount percentage must be greater than or equal to 0", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "100.0", inclusive = true, message = "Default discount percentage must be less than or equal to 100", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 3, fraction = 2, message = "Default discount percentage must have at most 3 integer digits and 2 decimal places", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal defaultDiscountPercentage,

    @Size(max = 500, message = "Comment must not exceed 500 characters", groups = {OnCreate.class, OnUpdate.class})
    String comment
) {}
