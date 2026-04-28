package PSG.backEnd.repository.forecast;

import PSG.backEnd.model.entity.forecast.BudgetForecastTemplateItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BudgetForecastTemplateItemRepository extends JpaRepository<BudgetForecastTemplateItem, Long> {

    List<BudgetForecastTemplateItem> findByTemplateIdOrderByRowOrderAsc(Long templateId);
}
