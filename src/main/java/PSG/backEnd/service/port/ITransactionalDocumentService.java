package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.TransactionalDocumentFilterDTO;
import PSG.backEnd.model.dto.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITransactionalDocumentService {

    TransactionalDocumentResponseDTO createTransactionalDocument(TransactionalDocumentDTO transactionalDocumentDTO);
    Page<TransactionalDocumentResponseDTO> getAllTransactionalDocuments(TransactionalDocumentFilterDTO filterDTO, Pageable pageable);
    TransactionalDocumentResponseDTO getTransactionalDocumentById(Long id);
    TransactionalDocumentResponseDTO updateTransactionalDocument(Long id, TransactionalDocumentDTO transactionalDocumentDTO);
    void deleteTransactionalDocument(Long id);
    TransactionalDocument getEntityById(Long id);
}