package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByIdAndDeletedFalse(Long id);
    Optional<Tenant> findByWebhookTokenAndDeletedFalse(String webhookToken);
    Optional<Tenant> findByCuit(String cuit);
    Optional<Tenant> findByCuitAndDeletedFalse(String cuit);
    Optional<Tenant> findByCuitAndDeletedTrue(String cuit);
    boolean existsByCuitAndDeletedFalse(String cuit);
    boolean existsByIdAndDeletedFalse(Long id);

    @Query("SELECT t FROM Tenant t " +
            "WHERE t.deleted = false " +
            "AND (:name IS NULL OR LOWER(CAST(t.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
            "AND (:cuit IS NULL OR LOWER(CAST(t.cuit AS string)) LIKE LOWER(CONCAT('%', CAST(:cuit AS string), '%'))) " +
            "AND (:active IS NULL OR t.active = :active) " +
            "AND (:search IS NULL OR (LOWER(CAST(t.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(t.cuit AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "     OR LOWER(CAST(t.legalName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<Tenant> findAllWithFilters(
            @Param("name") String name,
            @Param("cuit") String cuit,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
