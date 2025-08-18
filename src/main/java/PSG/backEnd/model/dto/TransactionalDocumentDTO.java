package PSG.backEnd.model.dto;

import PSG.backEnd.model.enums.DocumentType;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionalDocumentDTO(

    @NotNull(message = "Date is required", groups = OnCreate.class)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate date,

    @NotNull(message = "Document type is required", groups = OnCreate.class)
    @Valid
    DocumentType documentType,

    @NotBlank(message = "Branch code is required", groups = OnCreate.class)
    @Pattern(regexp = "\\d{5}", message = "Branch code must be exactly 5 digits", groups = {OnCreate.class, OnUpdate.class})
    String branchCode,

    @NotBlank(message = "Document number is required", groups = OnCreate.class)
    @Pattern(regexp = "\\d{8}", message = "Document number must be exactly 8 digits", groups = {OnCreate.class, OnUpdate.class})
    String documentNumber,

    @NotNull(message = "Supplier is required", groups = OnCreate.class)
    Long supplierId,

    @NotNull(message = "Other taxes amount is required", groups = OnCreate.class)
    @DecimalMin(value = "0.00", inclusive = true, message = "Other taxes cannot be negative", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 12, fraction = 2, message = "Other taxes must have up to 12 digits and 2 decimals", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal otherTaxes,

    @NotNull(message = "Discount percentage is required", groups = OnCreate.class)
    @DecimalMin(value = "0.00", inclusive = true, message = "Discount percentage cannot be negative", groups = {OnCreate.class, OnUpdate.class})
    @DecimalMax(value = "100.00", inclusive = true, message = "Discount percentage cannot exceed 100", groups = {OnCreate.class, OnUpdate.class})
    @Digits(integer = 3, fraction = 2, message = "Discount percentage must have up to 3 digits and 2 decimals", groups = {OnCreate.class, OnUpdate.class})
    BigDecimal discountPercentage,

    @Size(max = 500, message = "Comment cannot exceed 500 characters", groups = {OnCreate.class, OnUpdate.class})
    String comment,

    boolean deleted
) {}
