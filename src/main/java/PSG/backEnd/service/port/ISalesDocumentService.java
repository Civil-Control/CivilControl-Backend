package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.sales.SalesDocumentDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentFilterDTO;
import PSG.backEnd.model.dto.sales.SalesDocumentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISalesDocumentService {

    SalesDocumentResponseDTO createSalesDocument(SalesDocumentDTO dto);

    Page<SalesDocumentResponseDTO> getAllSalesDocuments(SalesDocumentFilterDTO filterDTO, Pageable pageable);

    SalesDocumentResponseDTO getSalesDocumentById(Long id);

    SalesDocumentResponseDTO updateSalesDocument(Long id, SalesDocumentDTO dto);

    SalesDocumentResponseDTO markAsPaid(Long id, boolean paid);

    void deleteSalesDocument(Long id);
}
