package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.forecast.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface IBudgetForecastService {

    BudgetForecastResponseDTO create(BudgetForecastDTO dto);

    BudgetForecastResponseDTO update(Long id, BudgetForecastDTO dto);

    BudgetForecastResponseDTO getById(Long id);

    Page<BudgetForecastSummaryDTO> getAll(BudgetForecastFilterDTO filter, Pageable pageable);

    void delete(Long id);

    // ── Estado ──
    BudgetForecastResponseDTO confirm(Long id);
    BudgetForecastResponseDTO reopen(Long id);

    // ── Items ──
    BudgetForecastItemResponseDTO addItem(Long forecastId, BudgetForecastItemDTO itemDTO);
    BudgetForecastItemResponseDTO updateItem(Long forecastId, Long itemId, BudgetForecastItemDTO itemDTO);
    void deleteItem(Long forecastId, Long itemId);

    // ── Aplicación ──
    BudgetForecastItemApplyResultDTO applyItem(Long forecastId, Long itemId);
    BudgetForecastBatchApplyResponseDTO applyBatch(Long forecastId, BudgetForecastItemBatchApplyDTO batchDTO);
    BudgetForecastItemResponseDTO revertItem(Long forecastId, Long itemId);
    BudgetForecastItemResponseDTO skipItem(Long forecastId, Long itemId, BudgetForecastItemSkipDTO skipDTO);

    // ── Instanciación desde plantilla ──
    BudgetForecastResponseDTO instantiateFromTemplate(BudgetForecastInstantiateDTO dto);

    // ── Import / Export ──
    BudgetForecastImportResultDTO importItemsFromExcel(Long forecastId, MultipartFile file, boolean dryRun);
    byte[] generateImportTemplate();
    byte[] exportToExcel(Long forecastId);
    byte[] exportToPdf(Long forecastId);
}
