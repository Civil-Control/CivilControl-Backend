package PSG.backEnd.repository.forecast;

import PSG.backEnd.model.entity.forecast.BudgetForecastItem;
import PSG.backEnd.model.enums.forecast.BudgetForecastItemApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetForecastItemRepository extends JpaRepository<BudgetForecastItem, Long> {

    Optional<BudgetForecastItem> findByIdAndBudgetForecastId(Long id, Long budgetForecastId);

    List<BudgetForecastItem> findByBudgetForecastIdOrderByRowOrderAsc(Long budgetForecastId);

    List<BudgetForecastItem> findByBudgetForecastIdAndApplicationStatus(
            Long budgetForecastId, BudgetForecastItemApplicationStatus status);

    long countByBudgetForecastIdAndApplicationStatus(
            Long budgetForecastId, BudgetForecastItemApplicationStatus status);
}
