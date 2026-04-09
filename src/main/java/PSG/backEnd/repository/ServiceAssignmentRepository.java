package PSG.backEnd.repository;

import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.enums.SubjectType;
import PSG.backEnd.model.enums.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceAssignmentRepository extends JpaRepository<ServiceAssignment, Long> {

    List<ServiceAssignment> findByDeletedFalse();
    Optional<ServiceAssignment> findByIdAndDeletedFalse(Long id);
    boolean existsByIdAndDeletedFalse(Long id);

    boolean existsByAccountNumberAndDeletedFalse(String accountNumber);
    boolean existsByAccountNumberAndDeletedFalseAndIdNot(String accountNumber, Long id);

    @Query("SELECT sa FROM ServiceAssignment sa " +
            "LEFT JOIN sa.building b " +
            "LEFT JOIN sa.vehicle v " +
            "WHERE sa.deleted = false " +
            "AND (:subjectType IS NULL OR sa.subjectType = :subjectType) " +
            "AND (CAST(:serviceSupplierId AS long) IS NULL OR sa.serviceSupplier.id = :serviceSupplierId) " +
            "AND (CAST(:buildingId AS long) IS NULL OR b.id = :buildingId) " +
            "AND (CAST(:vehicleId AS long) IS NULL OR v.id = :vehicleId) " +
            "AND (:serviceType IS NULL OR sa.serviceType = :serviceType) " +
            "AND (:search IS NULL OR (" +
            "     LOWER(CAST(sa.serviceSupplier.supplier.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sa.serviceSupplier.supplier.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(b.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(v.licensePlate AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sa.accountHolder AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sa.accountNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            "))")
    Page<ServiceAssignment> findAllWithFilters(
            @Param("subjectType") SubjectType subjectType,
            @Param("serviceSupplierId") Long serviceSupplierId,
            @Param("buildingId") Long buildingId,
            @Param("vehicleId") Long vehicleId,
            @Param("serviceType") ServiceType serviceType,
            @Param("search") String search,
            Pageable pageable
    );
}
