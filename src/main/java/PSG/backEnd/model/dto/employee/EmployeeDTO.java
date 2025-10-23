package PSG.backEnd.model.dto.employee;

import PSG.backEnd.model.dto.address.AddressDTO;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;
import PSG.backEnd.model.validation.ValidationGroups.OnCreate;
import PSG.backEnd.model.validation.ValidationGroups.OnUpdate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record EmployeeDTO(
    @NotBlank(message = "Name cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "Name must contain only letters, spaces, dots, hyphens and apostrophes", groups = {OnCreate.class, OnUpdate.class})
    String name,

    @NotBlank(message = "Last name cannot be blank", groups = OnCreate.class)
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters", groups = {OnCreate.class, OnUpdate.class})
    @Pattern(regexp = "^[\\p{L}\\s.'-]+$", message = "Last name must contain only letters, spaces, dots, hyphens and apostrophes", groups = {OnCreate.class, OnUpdate.class})
    String lastName,

    @NotBlank(message = "DNI cannot be blank", groups = OnCreate.class)
    @Pattern(regexp = "^\\d{7,8}$", message = "DNI must be 7 or 8 digits", groups = {OnCreate.class, OnUpdate.class})
    String dni,

    @NotBlank(message = "CUIL cannot be blank", groups = OnCreate.class)
    @Pattern(regexp = "^\\d{2}-\\d{7,8}-\\d$", message = "CUIL must have XX-XXXXXXXX-X format", groups = {OnCreate.class, OnUpdate.class})
    String cuil,

    @NotNull(message = "Project area ID cannot be null", groups = OnCreate.class)
    Long projectAreaId,

    @NotNull(message = "Address cannot be null", groups = OnCreate.class)
    @Valid
    AddressDTO address,

    @NotNull(message = "Birth date cannot be null", groups = OnCreate.class)
    @Past(message = "Birth date must be in the past", groups = {OnCreate.class, OnUpdate.class})
    LocalDate birthDate,

    @NotBlank(message = "Phone number cannot be blank", groups = OnCreate.class)
    @Pattern(regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$",
             message = "Invalid phone number format",
             groups = {OnCreate.class, OnUpdate.class})
    String phoneNumber,

    @NotBlank(message = "Email cannot be blank", groups = OnCreate.class)
    @Email(message = "Email must be valid", groups = {OnCreate.class, OnUpdate.class})
    @Size(max = 100, message = "Email must not exceed 100 characters", groups = {OnCreate.class, OnUpdate.class})
    String email,

    @NotNull(message = "Emergency contact cannot be null", groups = OnCreate.class)
    @Valid
    EmergencyContactDTO emergencyContact,

    @NotNull(message = "Employment type cannot be null", groups = OnCreate.class)
    EmploymentType employmentType,

    @NotNull(message = "Employee status cannot be null", groups = OnCreate.class)
    EmployeeStatus employeeStatus,

    @NotNull(message = "Employee role cannot be null", groups = OnCreate.class)
    EmployeeRole employeeRole,

    @NotNull(message = "Hire date cannot be null", groups = OnCreate.class)
    LocalDate hireDate,

    LocalDate endDate
) {}

