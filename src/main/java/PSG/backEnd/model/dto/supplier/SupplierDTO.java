package PSG.backEnd.model.dto.supplier;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.dto.contactInfo.ContactInfoDTO;
import PSG.backEnd.model.enums.documents.PaymentMethod;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating suppliers. " +
        "Represents business entities that provide goods or services to the organization.")
public record SupplierDTO(

    @Schema(description = "Argentine tax identification number (Código Único de Identificación Tributaria). " +
            "Must follow the format XX-XXXXXXXX-X where X represents digits. " +
            "This is the unique tax identifier for businesses in Argentina.",
            example = "30-12345678-9",
            pattern = "^\\d{2}-\\d{8}-\\d$",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "CUIT cannot be blank", groups = OnCreate.class)
    @Pattern(regexp = "^\\d{2}-\\d{8}-\\d$", message = "CUIT must have XX-XXXXXXXX-X format", groups = {OnCreate.class, OnUpdate.class})
    String cuit,

    @Schema(description = "Official legal name of the supplier company as registered with tax authorities. " +
            "This is the formal business name. Minimum 1 character, maximum 50 characters.",
            example = "Construcciones García S.A.",
            minLength = 1,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Legal name cannot be blank", groups = OnCreate.class)
    @Size(min = 1, max = 50, message = "Legal name must be between 1 and 50 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^\\s*\\S.*$", message = "Legal name cannot be blank or only spaces", groups = {OnCreate.class, OnUpdate.class})
    String legalName,

    @Schema(description = "Commercial or trade name used by the supplier for business operations. " +
            "This is the brand name or business name commonly used. Minimum 1 character, maximum 50 characters.",
            example = "García Construcciones",
            minLength = 1,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Trade name cannot be blank", groups = OnCreate.class)
    @Size(min = 1, max = 50, message = "Trade name must be between 1 and 50 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^\\s*\\S.*$", message = "Trade name cannot be blank or only spaces", groups = {OnCreate.class, OnUpdate.class})
    String tradeName,

    @Schema(description = "List of payment methods accepted by this supplier. At least one method must be specified. " +
            "Valid values: EFECTIVO (Cash), TRANSFERENCIA (Bank transfer), CHEQUE (Check). " +
            "This defines how payments to this supplier can be made.",
            example = "[\"EFECTIVO\", \"TRANSFERENCIA\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "At least one payment method must be specified", groups = OnCreate.class)
    List<@NotNull(message = "Payment method cannot be null", groups = {OnCreate.class, OnUpdate.class}) PaymentMethod> allowedPaymentMethods,

    @Schema(description = "Complete physical address of the supplier including street, number, city, province, and postal code.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Address cannot be null", groups = OnCreate.class)
    @Valid
    AddressDTO address,

    @Schema(description = "Contact information for the supplier including phone numbers and email addresses. " +
            "Optional but recommended for communication purposes.",
            nullable = true)
    @Valid
    ContactInfoDTO contactInfo,

    @Schema(description = "Default discount percentage that this supplier typically offers. " +
            "Must be between 0 and 100. Used as a reference for purchase orders and negotiations.",
            example = "5.50",
            minimum = "0.0",
            maximum = "100.0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Default discount percentage cannot be null", groups = OnCreate.class)
    @DecimalMin(value = "0.0", inclusive = true, message = "Default discount percentage must be greater than or equal to 0", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "100.0", inclusive = true, message = "Default discount percentage must be less than or equal to 100", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 3, fraction = 2, message = "Default discount percentage must have at most 3 integer digits and 2 decimal places", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal defaultDiscountPercentage,

    @Schema(description = "Additional notes or comments about the supplier. Can include payment terms, delivery preferences, " +
            "or any other relevant information. Maximum 500 characters. Optional field.",
            example = "Preferir entregas los lunes. Pago contra factura.",
            maxLength = 500,
            nullable = true)
    @Size(max = 500, message = "Comment must not exceed 500 characters", groups = {OnCreate.class, OnUpdate.class})
    String comment
) {}
