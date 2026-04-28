package PSG.backEnd.repository.forecast;

import PSG.backEnd.model.entity.forecast.BudgetForecast;
import PSG.backEnd.model.enums.forecast.BudgetForecastStatus;
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
public interface BudgetForecastRepository extends JpaRepository<BudgetForecast, Long> {

    Optional<BudgetForecast> findByIdAndDeletedFalse(Long id);

    @Query("SELECT bf FROM BudgetForecast bf " +
           "WHERE bf.deleted = false " +
           "AND (CAST(:periodFromGte AS date) IS NULL OR bf.periodTo   >= :periodFromGte) " +
           "AND (CAST(:periodToLte   AS date) IS NULL OR bf.periodFrom <= :periodToLte) " +
           "AND (:statuses IS NULL OR bf.status IN :statuses) " +
           "AND (:search IS NULL OR LOWER(bf.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(COALESCE(bf.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:hasAppliedItems IS NULL OR " +
           "     (:hasAppliedItems = true  AND bf.appliedAmount > 0) OR " +
           "     (:hasAppliedItems = false AND bf.appliedAmount = 0))")
    Page<BudgetForecast> findAllWithFilters(
            @Param("periodFromGte") LocalDate periodFromGte,
            @Param("periodToLte") LocalDate periodToLte,
            @Param("statuses") List<BudgetForecastStatus> statuses,
            @Param("search") String search,
            @Param("hasAppliedItems") Boolean hasAppliedItems,
            Pageable pageable
    );
}
