package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import PSG.backEnd.model.validation.ValidEmergencyContact;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Data Transfer Object for creating or updating an employee. " +
        "Represents complete employee information including personal details, contact information, " +
        "employment details, and emergency contact.")
@ValidEmergencyContact(groups = {OnCreate.class, OnUpdate.class})
public record EmployeeDTO(
    @Schema(description = "Employee's first name. Must contain only letters, spaces, dots, hyphens and apostrophes. " +
            "Minimum 2 characters, maximum 100 characters.",
            example = "Juan Carlos",
            minLength = 2,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "{employee.name.size}", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
    String name,

    @Schema(description = "Employee's last name or surname. Must contain only letters, spaces, dots, hyphens and apostrophes. " +
            "Minimum 2 characters, maximum 100 characters.",
            example = "García Pérez",
            minLength = 2,
            maxLength = 100,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.notBlank}", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "{employee.lastName.size}", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "{validation.pattern}", groups = {OnCreate.class, OnUpdate.class})
    String lastName,

    @Schema(description = "Argentine national identity document number (Documento Nacional de Identidad). " +
            "Must be 7 or 8 digits without dots or spaces.",
            example = "12345678",
            pattern = "^\\d{7,8}$",
            minLength = 7,
            maxLength = 8,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^\\d{7,8}$", message = "{employee.dni.invalid}", groups = {OnCreate.class, OnUpdate.class})
    String dni,

    @Schema(description = "Argentine tax identification number (Código Único de Identificación Laboral). " +
            "Must follow the format XX-XXXXXXXX-X where X represents digits.",
            example = "20-12345678-9",
            pattern = "^\\d{2}-\\d{7,8}-\\d$",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "^\\d{2}-\\d{7,8}-\\d$", message = "{employee.cuil.invalid}", groups = {OnCreate.class, OnUpdate.class})
    String cuil,

    @Schema(description = "Unique identifier of the project area where the employee is assigned. " +
            "Must reference an existing project area in the system.",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    Long projectAreaId,

    @Schema(description = "Complete residential address of the employee including street, number, city, province, and postal code.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    AddressDTO address,

    @Schema(description = "Employee's date of birth. Must be a date in the past. Used for age calculation and legal requirements.",
            example = "1990-05-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @Past(message = "{employee.birthDate.past}", groups = {OnCreate.class, OnUpdate.class})
    LocalDate birthDate,

    @Schema(description = "Employee's primary phone number. Can include country code, area code, and must be in valid phone format. " +
            "Accepts various international formats.",
            example = "+54 9 11 1234-5678",
            nullable = true)
    @Pattern(regexp = "^$|^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$",
             message = "{contactInfo.phoneNumber.invalid}",
             groups = {OnCreate.class, OnUpdate.class})
    String phoneNumber,

    @Schema(description = "Employee's email address for official communications. Must be a valid email format. " +
            "Maximum 100 characters.",
            example = "juan.garcia@example.com",
            format = "email",
            maxLength = 100,
            nullable = true)
    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$",
             message = "{employee.email.invalid}",
             groups = {OnCreate.class, OnUpdate.class})
    @Size(max = 100, message = "{employee.email.size}", groups = {OnCreate.class, OnUpdate.class})
    String email,

    @Schema(description = "Emergency contact information for the employee. Includes name, relationship, and phone number " +
            "of a person to contact in case of emergency.",
            nullable = true)
    @Valid
    EmergencyContactDTO emergencyContact,

    @Schema(description = "Type of employment contract. Valid values: " +
            "TIEMPO_COMPLETO (full-time employee), " +
            "MEDIO_TIEMPO (part-time employee), " +
            "CONTRATO_TEMPORAL (fixed-term contract), " +
            "PASANTIA (internship), " +
            "SUB_CONTRATADO (subcontracted).",
            example = "TIEMPO_COMPLETO",
            allowableValues = {"TIEMPO_COMPLETO", "MEDIO_TIEMPO", "CONTRATO_TEMPORAL", "PASANTIA", "SUB_CONTRATADO"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    EmploymentType employmentType,

    @Schema(description = "Current employment status of the employee. Valid values: " +
            "ACTIVO (currently working), " +
            "LICENCIA (on leave), " +
            "SUSPENDIDO (temporarily suspended from duties).",
            example = "ACTIVO",
            allowableValues = {"ACTIVO", "LICENCIA", "SUSPENDIDO"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    EmployeeStatus employeeStatus,

    @Schema(description = "Employee's roles or job positions in the organization. One or more roles can be assigned. " +
            "Valid values: CHOFER, OFICIAL, AYUDANTE, ADMINISTRATIVO, LIMPIEZA, MECANICO, OTRO.",
            example = "[\"CHOFER\", \"OFICIAL\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "{validation.notNull}", groups = OnCreate.class)
    @NotEmpty(message = "{employee.employeeRole.required}", groups = OnCreate.class)
    List<EmployeeRole> employeeRoles,

    @Schema(description = "Date when the employee was hired or started working for the organization. " +
            "Used for seniority calculations and employment history.",
            example = "2023-01-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate hireDate,

    @Schema(description = "Optional date when the employee's employment ended or contract terminated. " +
            "Leave null for currently employed personnel. Set this date when an employee leaves the organization.",
            example = "2025-12-31",
            nullable = true)
    LocalDate endDate
) {}

