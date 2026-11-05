package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateDTO;
import PSG.backEnd.model.dto.forecast.BudgetForecastTemplateResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IBudgetForecastTemplateService {

    BudgetForecastTemplateResponseDTO create(BudgetForecastTemplateDTO dto);

    BudgetForecastTemplateResponseDTO update(Long id, BudgetForecastTemplateDTO dto);

    BudgetForecastTemplateResponseDTO getById(Long id);

    Page<BudgetForecastTemplateResponseDTO> getAll(Boolean active, String search, Pageable pageable);

    void delete(Long id);
}
