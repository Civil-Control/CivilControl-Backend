package PSG.backEnd.repository.forecast;

import PSG.backEnd.model.entity.forecast.BudgetForecastTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetForecastTemplateRepository extends JpaRepository<BudgetForecastTemplate, Long> {

    Optional<BudgetForecastTemplate> findByIdAndDeletedFalse(Long id);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    @Query("SELECT t FROM BudgetForecastTemplate t " +
           "WHERE t.deleted = false " +
           "AND (:active IS NULL OR t.active = :active) " +
           "AND (:search IS NULL OR LOWER(CAST(t.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<BudgetForecastTemplate> findAllWithFilters(
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
