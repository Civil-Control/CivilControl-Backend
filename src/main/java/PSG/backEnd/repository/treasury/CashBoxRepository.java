package PSG.backEnd.repository.treasury;

import PSG.backEnd.model.entity.treasury.CashBox;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CashBoxRepository extends JpaRepository<CashBox, Long> {

    Optional<CashBox> findByIdAndDeletedFalse(Long id);

    boolean existsByDeletedFalseAndActiveTrue();

    @Query("SELECT cb FROM CashBox cb WHERE cb.deleted = false " +
            "AND (:active IS NULL OR cb.active = :active) " +
            "AND (:search IS NULL OR LOWER(cb.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "                    OR LOWER(COALESCE(cb.description, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CashBox> findAllWithFilters(@Param("active") Boolean active,
                                     @Param("search") String search,
                                     Pageable pageable);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndDeletedFalse(String name, Long id);
}
