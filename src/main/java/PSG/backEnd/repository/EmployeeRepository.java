package PSG.backEnd.repository;

import PSG.backEnd.model.entity.employee.Employee;
import PSG.backEnd.model.enums.employee.EmployeeRole;
import PSG.backEnd.model.enums.employee.EmployeeStatus;
import PSG.backEnd.model.enums.employee.EmploymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByDeletedFalse();
    Optional<Employee> findByIdAndDeletedFalse(Long id);
    Optional<Employee> findByDniAndDeletedTrue(String dni);
    Optional<Employee> findByCuilAndDeletedTrue(String cuil);
    boolean existsByDniAndDeletedFalse(String dni);
    boolean existsByCuilAndDeletedFalse(String cuil);
    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT e FROM Employee e " +
            "WHERE (:name IS NULL OR LOWER(CAST(e.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
            "AND (:lastName IS NULL OR LOWER(CAST(e.lastName AS string)) LIKE LOWER(CONCAT('%', CAST(:lastName AS string), '%'))) " +
            "AND (:dni IS NULL OR e.dni LIKE %:dni%) " +
            "AND (:cuil IS NULL OR e.cuil LIKE %:cuil%) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR e.projectArea.id = :projectAreaId) " +
            "AND (:city IS NULL OR LOWER(CAST(e.address.city AS string)) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%'))) " +
            "AND (CAST(:employmentType AS string) IS NULL OR e.employmentType = :employmentType) " +
            "AND (CAST(:employeeStatus AS string) IS NULL OR e.employeeStatus = :employeeStatus) " +
            "AND (CAST(:employeeRole AS string) IS NULL OR e.employeeRole = :employeeRole) " +
            "AND (CAST(:hireDateFrom AS date) IS NULL OR e.hireDate >= :hireDateFrom) " +
            "AND (CAST(:hireDateTo AS date) IS NULL OR e.hireDate <= :hireDateTo) " +
            "AND e.deleted = false")
    Page<Employee> findAllWithFilters(
            @Param("name") String name,
            @Param("lastName") String lastName,
            @Param("dni") String dni,
            @Param("cuil") String cuil,
            @Param("projectAreaId") Long projectAreaId,
            @Param("city") String city,
            @Param("employmentType") EmploymentType employmentType,
            @Param("employeeStatus") EmployeeStatus employeeStatus,
            @Param("employeeRole") EmployeeRole employeeRole,
            @Param("hireDateFrom") LocalDate hireDateFrom,
            @Param("hireDateTo") LocalDate hireDateTo,
            Pageable pageable
    );
}

