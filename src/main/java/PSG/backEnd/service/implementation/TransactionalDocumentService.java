package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.supplier.SupplierNotFoundException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentAlreadyActiveException;
import PSG.backEnd.exception.transactionalDocument.TransactionalDocumentNotFoundException;
import PSG.backEnd.model.dto.*;
import PSG.backEnd.model.entity.*;
import PSG.backEnd.model.enums.DocumentType;
import PSG.backEnd.model.enums.PaymentMethod;
import PSG.backEnd.model.mapper.TransactionalDocumentMapper;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import PSG.backEnd.service.port.ISupplierService;
import PSG.backEnd.service.port.ITransactionalDocumentService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionalDocumentService implements ITransactionalDocumentService {

    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final TransactionalDocumentMapper transactionalDocumentMapper;

    private final ISupplierService iSupplierService;

    @Override
    @Transactional
    public TransactionalDocumentResponseDTO createTransactionalDocument(TransactionalDocumentDTO dto) {
        // Validate the DTO and check for existing documents
        validateNewDocument(dto);

        // Create a unique key for the document based on branch code and document number
        String documentKey = dto.branchCode() + " - " + dto.documentNumber();

        // Check if an active document with the same branch code and document number already exists
        if (transactionalDocumentRepository
                .existsByBranchCodeAndDocumentNumberAndSupplierIdAndDeletedFalse(
                        dto.branchCode(),
                        dto.documentNumber(),
                        dto.supplierId())) {
            throw new TransactionalDocumentAlreadyActiveException(
                    "There is already an active transactional document with number "
                            + dto.branchCode() + "-" + dto.documentNumber()
                            + " for supplierId " + dto.supplierId());
        }

        // Check if a deleted document with the same branch code and document number exists
        Optional<TransactionalDocument> deletedDocument = transactionalDocumentRepository
                .findByBranchCodeAndDocumentNumberAndDeletedTrue(dto.branchCode(), dto.documentNumber());

        // If a deleted document exists, reactivate it; otherwise, create a new one
        TransactionalDocument document;
        document = deletedDocument
                .map(transactionalDocument -> reactivateExistingDocument(transactionalDocument, dto)).orElseGet(() -> createNewDocument(dto));

        document = transactionalDocumentRepository.save(document);

        return transactionalDocumentMapper.toResponseDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionalDocumentResponseDTO> getAllTransactionalDocuments(TransactionalDocumentFilterDTO filterDTO, Pageable pageable) {
        return transactionalDocumentRepository.findAllWithFilters(
                filterDTO.documentNumber(),
                filterDTO.supplierCuit(),
                filterDTO.supplierName(),
                filterDTO.maxTotalAmount(),
                filterDTO.minTotalAmount(),
                filterDTO.totalAmount(),
                filterDTO.fromDate(),
                filterDTO.toDate(),
                pageable
        ).map(transactionalDocumentMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionalDocumentResponseDTO getTransactionalDocumentById(Long id) {
        return transactionalDocumentRepository.findByIdAndDeletedFalse(id)
                .map(transactionalDocumentMapper::toResponseDto)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionalDocument getEntityById(Long id) {
        return transactionalDocumentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new TransactionalDocumentNotFoundException(id));
    }

    @Override
    @Transactional
    public TransactionalDocumentResponseDTO updateTransactionalDocument(Long id, TransactionalDocumentDTO dto) {
        TransactionalDocument existingDocument = getEntityById(id);

        transactionalDocumentMapper.partialUpdate(dto, existingDocument);

        TransactionalDocument savedDocument = transactionalDocumentRepository.save(existingDocument);
        return transactionalDocumentMapper.toResponseDto(savedDocument);
    }

    @Override
    @Transactional
    public void deleteTransactionalDocument(Long id) {
        TransactionalDocument document = getEntityById(id);
        document.setDeleted(true);

        transactionalDocumentRepository.save(document);
    }

    /// private auxiliary methods

    private TransactionalDocument createNewDocument(TransactionalDocumentDTO dto) {
        TransactionalDocument document = transactionalDocumentMapper.toEntity(dto);
        document.setDeleted(false);

        Supplier supplier = iSupplierService.getEntityById(dto.supplierId());
        document.setSupplier(supplier);

        if (isInvoice(document.getDocumentType())) {
            List<PaymentMethod> methods = supplier.getAllowedPaymentMethods();
            boolean paid = methods != null
                    && methods.size() == 1
                    && methods.contains(PaymentMethod.CASH);
            document.setPaid(paid);

            if (!paid) {
                BigDecimal pendingBalance = supplier.getPendingBalance() != null
                        ? supplier.getPendingBalance()
                        : BigDecimal.ZERO;
                supplier.setPendingBalance(pendingBalance.add(document.getTotal()));
            }
        } else {
            document.setPaid(true);
        }

        return document;
    }

    private TransactionalDocument reactivateExistingDocument(TransactionalDocument document, TransactionalDocumentDTO dto) {
        transactionalDocumentMapper.partialUpdate(dto, document);
        document.setDeleted(false);

        return document;
    }

    private void validateNewDocument(TransactionalDocumentDTO dto) {
        Supplier supplier = iSupplierService.getEntityById(dto.supplierId());
        if (supplier == null) {
            throw new SupplierNotFoundException(dto.supplierId());
        }
    }

    private boolean isInvoice(DocumentType type) {
        return type == DocumentType.BILL_A
                || type == DocumentType.BILL_B
                || type == DocumentType.BILL_C;
    }

}
