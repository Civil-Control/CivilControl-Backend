package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentFilterDTO;
import PSG.backEnd.model.dto.transactionalDocument.TransactionalDocumentResponseDTO;
import PSG.backEnd.model.entity.TransactionalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ITransactionalDocumentService {

    TransactionalDocumentResponseDTO createTransactionalDocument(TransactionalDocumentDTO transactionalDocumentDTO);
    Page<TransactionalDocumentResponseDTO> getAllTransactionalDocuments(TransactionalDocumentFilterDTO filterDTO, Pageable pageable);
    TransactionalDocumentResponseDTO getTransactionalDocumentById(Long id);
    TransactionalDocumentResponseDTO updateTransactionalDocument(Long id, TransactionalDocumentDTO transactionalDocumentDTO);
    void updateTransactionalDocumentStatus(Long documentId, Long supplierId, BigDecimal amount);
    void revertTransactionalDocumentStatusIfExists(Long documentId, Long supplierId);
    void deleteTransactionalDocument(Long id);
    TransactionalDocument getEntityById(Long id);
}