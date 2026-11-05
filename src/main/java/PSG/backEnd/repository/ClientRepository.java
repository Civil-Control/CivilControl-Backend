package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Client;
import PSG.backEnd.model.enums.IvaCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByDeletedFalse();

    Optional<Client> findByIdAndDeletedFalse(Long id);

    Optional<Client> findByCuitAndTenantIdAndDeletedFalse(String cuit, Long tenantId);

    boolean existsByCuitAndDeletedFalse(String cuit);

    boolean existsByBusinessNameAndDeletedFalse(String businessName);

    @Query("SELECT c FROM Client c " +
           "WHERE (:cuit IS NULL OR LOWER(CAST(c.cuit AS string)) LIKE LOWER(CONCAT('%', CAST(:cuit AS string), '%'))) " +
           "AND (:businessName IS NULL OR LOWER(CAST(c.businessName AS string)) LIKE LOWER(CONCAT('%', CAST(:businessName AS string), '%'))) " +
           "AND (:tradeName IS NULL OR LOWER(CAST(c.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:tradeName AS string), '%'))) " +
           "AND (:ivaCondition IS NULL OR c.ivaCondition = :ivaCondition) " +
           "AND (:active IS NULL OR c.active = :active) " +
           "AND c.deleted = false " +
           "AND (:search IS NULL OR (" +
           "     LOWER(CAST(c.cuit AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "     OR LOWER(CAST(c.businessName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "     OR LOWER(CAST(c.tradeName AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<Client> findAllWithFilters(
            @Param("cuit") String cuit,
            @Param("businessName") String businessName,
            @Param("tradeName") String tradeName,
            @Param("ivaCondition") IvaCondition ivaCondition,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
