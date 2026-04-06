package PSG.backEnd.repository;

import PSG.backEnd.model.entity.serviceSupplier.ServiceAssignment;
import PSG.backEnd.model.enums.ServiceCategory;
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

    boolean existsByServiceSupplier_IdAndBuilding_IdAndServiceTypeAndDeletedFalse(
            Long serviceSupplierId, Long buildingId, ServiceType serviceType);

    Optional<ServiceAssignment> findByServiceSupplier_IdAndBuilding_IdAndServiceTypeAndDeletedTrue(
            Long serviceSupplierId, Long buildingId, ServiceType serviceType);

    @Query("SELECT sa FROM ServiceAssignment sa " +
            "WHERE sa.deleted = false " +
            "AND (CAST(:serviceSupplierId AS long) IS NULL OR sa.serviceSupplier.id = :serviceSupplierId) " +
            "AND (CAST(:buildingId AS long) IS NULL OR sa.building.id = :buildingId) " +
            "AND (:serviceType IS NULL OR sa.serviceType = :serviceType) " +
            "AND (:serviceCategory IS NULL OR sa.serviceCategory = :serviceCategory) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR sa.projectArea.id = :projectAreaId) " +
            "AND (:search IS NULL OR (" +
            "     LOWER(CAST(sa.serviceSupplier.supplier.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sa.serviceSupplier.supplier.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sa.building.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sa.accountHolder AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(sa.accountNumber AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))" +
            "))")
    Page<ServiceAssignment> findAllWithFilters(
            @Param("serviceSupplierId") Long serviceSupplierId,
            @Param("buildingId") Long buildingId,
            @Param("serviceType") ServiceType serviceType,
            @Param("serviceCategory") ServiceCategory serviceCategory,
            @Param("projectAreaId") Long projectAreaId,
            @Param("search") String search,
            Pageable pageable
    );
}
