package PSG.backEnd.model.entity.employee;

import PSG.backEnd.model.entity.Address;
import PSG.backEnd.model.entity.ProjectArea;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "employees")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String dni;

    @Column(unique = true, nullable = false)
    private String cuil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_area_id")
    private ProjectArea projectArea;

    @Embedded
    private Address address;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column
    private String email;

    @Embedded
    private EmergencyContact emergencyContact;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_status", nullable = false)
    private EmployeeStatus employeeStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_role", nullable = false)
    private EmployeeRole employeeRole;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean deleted;
}
