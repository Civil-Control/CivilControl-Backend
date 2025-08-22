package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.payment.PaymentNotFoundException;
import PSG.backEnd.model.dto.payment.*;
import PSG.backEnd.model.entity.payment.*;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.mapper.*;
import PSG.backEnd.repository.PaymentRepository.CashPaymentRepository;
import PSG.backEnd.repository.PaymentRepository.CheckPaymentRepository;
import PSG.backEnd.repository.PaymentRepository.PaymentRepository;
import PSG.backEnd.repository.PaymentRepository.TransferPaymentRepository;
import PSG.backEnd.service.port.IPaymentService;
import PSG.backEnd.service.port.ISupplierService;
import PSG.backEnd.service.port.ITransactionalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class PaymentService implements IPaymentService {

    private final PaymentRepository paymentRepository;
    private final CashPaymentRepository cashPaymentRepository;
    private final TransferPaymentRepository transferPaymentRepository;
    private final CheckPaymentRepository checkPaymentRepository;
    private final CashPaymentMapper cashPaymentMapper;
    private final TransferPaymentMapper transferPaymentMapper;
    private final CheckPaymentMapper checkPaymentMapper;

    private final ISupplierService iSupplierService;
    private final ITransactionalDocumentService iTransactionalDocumentService;

    @Override
    @Transactional
    public CashPaymentResponseDTO createCash(CashPaymentDTO dto) {
        return createPayment(
            dto.paymentDetails(),
            cashPaymentMapper::toEntityOnCreate,
            cashPaymentRepository::save,
            cashPaymentMapper::toResponse,
            dto
        );
    }

    @Override
    @Transactional
    public TransferPaymentResponseDTO createTransfer(TransferPaymentDTO dto) {
        return createPayment(
            dto.paymentDetails(),
            transferPaymentMapper::toEntityOnCreate,
            transferPaymentRepository::save,
            transferPaymentMapper::toResponse,
            dto
        );
    }

    @Override
    @Transactional
    public CheckPaymentResponseDTO createCheck(CheckPaymentDTO dto) {
        return createPayment(
            dto.paymentDetails(),
            checkPaymentMapper::toEntityOnCreate,
            checkPaymentRepository::save,
            checkPaymentMapper::toResponse,
            dto
        );
    }

    /**
     * Template method que maneja la lógica común de creación de pagos
     * Respeta el principio DRY y SRP
     */
    private <T, R extends PaymentResponseDTO, D> R createPayment(
        PaymentDetailsDTO paymentDetails,
        Function<D, T> entityMapper,
        Function<T, T> repository,
        Function<T, R> responseMapper,
        D dto
    ) {
        // 1. Crear la entidad específica del tipo de pago
        T entity = entityMapper.apply(dto);

        // 2. Ejecutar lógica de negocio común (aplicar Information Expert - GRASP)
        executePaymentBusinessLogic(paymentDetails);

        // 3. Guardar y retornar respuesta
        T savedEntity = repository.apply(entity);
        return responseMapper.apply(savedEntity);
    }

    /**
     * Método que encapsula toda la lógica de negocio común
     * Respeta el principio de Information Expert (GRASP)
     */
    private void executePaymentBusinessLogic(PaymentDetailsDTO paymentDetails) {
        // Actualizar balance del proveedor
        iSupplierService.updateSupplierBalance(
            paymentDetails.supplierId(),
            paymentDetails.amount()
        );

        // Actualizar estado de documentos pagados
        updatePaidDocuments(paymentDetails);
    }

    /**
     * Revierte los efectos de un pago en el sistema
     * Usado para UPDATE y DELETE operations
     */
    private void revertPaymentBusinessLogic(PaymentDetailsDTO originalPaymentDetails) {
        // Revertir balance del proveedor (restar el monto del pago)
        iSupplierService.updateSupplierBalance(
            originalPaymentDetails.supplierId(),
            originalPaymentDetails.amount().negate() // Negativo para restar
        );

        // Revertir estado de documentos (marcarlos como no pagados)
        revertPaidDocuments(originalPaymentDetails);
    }

    /**
     * Marca documentos como no pagados (revierte el pago)
     * Respeta SRP - Single Responsibility Principle
     */
    private void revertPaidDocuments(PaymentDetailsDTO paymentDetails) {
        // Verificar si hay documentos para revertir (pagos independientes no tienen documentos)
        if (paymentDetails.paidDocumentIds() != null && !paymentDetails.paidDocumentIds().isEmpty()) {
            for (Long documentId : paymentDetails.paidDocumentIds()) {
                try {
                    // Revertir directamente usando monto 0 para indicar reversión
                    iTransactionalDocumentService.updateTransactionalDocumentStatus(
                        documentId,
                        paymentDetails.supplierId(),
                        BigDecimal.ZERO // Monto 0 indica revertir
                    );
                } catch (Exception e) {
                    // Log error pero continúa con otros documentos
                    System.err.println("Error reverting document " + documentId + ": " + e.getMessage());
                }
            }
        }
        // Si paidDocumentIds es null o vacío, es un pago independiente - no revierte documentos
    }

    /**
     * Método responsable de actualizar los documentos pagados
     * Respeta SRP - Single Responsibility Principle
     */
    private void updatePaidDocuments(PaymentDetailsDTO paymentDetails) {
        // Verificar si hay documentos para actualizar (pagos independientes no tienen documentos)
        if (paymentDetails.paidDocumentIds() != null && !paymentDetails.paidDocumentIds().isEmpty()) {
            for (Long documentId : paymentDetails.paidDocumentIds()) {
                iTransactionalDocumentService.updateTransactionalDocumentStatus(
                    documentId,
                    paymentDetails.supplierId(),
                    paymentDetails.amount()
                );
            }
        }
        // Si paidDocumentIds es null o vacío, es un pago independiente - no hace nada con documentos
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDTO> findAll(PaymentFilterDTO filter, Pageable pageable) {
        return paymentRepository.findAllWithFilters(
                filter.paymentMethod(),
                filter.startDate(),
                filter.endDate(),
                filter.minAmount(),
                filter.maxAmount(),
                filter.transactionNumber(),
                filter.supplierId(),
                pageable
        ).map(this::mapToPaymentResponse);
    }

    /**
     * Factory method para mapear PaymentDetails a PaymentResponseDTO
     * Respeta el patrón Factory y OCP
     */
    private PaymentResponseDTO mapToPaymentResponse(PaymentDetails paymentDetails) {
        if (paymentDetails.getCashPayment() != null) {
            return cashPaymentMapper.toResponse(paymentDetails.getCashPayment());
        } else if (paymentDetails.getTransferPayment() != null) {
            return transferPaymentMapper.toResponse(paymentDetails.getTransferPayment());
        } else if (paymentDetails.getCheckPayment() != null) {
            return checkPaymentMapper.toResponse(paymentDetails.getCheckPayment());
        } else {
            throw new IllegalStateException("PaymentDetails with ID " + paymentDetails.getId() + " has no associated payment type");
        }
    }

    @Override
    @Transactional
    public CashPaymentResponseDTO updateCash(Long id, CashPaymentDTO dto) {
        return updatePaymentWithBusinessLogic(
            id,
            cashPaymentRepository,
            "Cash payment not found",
            entity -> {
                // Obtener datos originales antes de la actualización
                PaymentDetailsDTO originalDetails = extractPaymentDetails(entity);
                // Actualizar la entidad
                cashPaymentMapper.updateEntityFromDto(dto, entity);
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails());
            },
            cashPaymentMapper::toResponse
        );
    }

    @Override
    @Transactional
    public TransferPaymentResponseDTO updateTransfer(Long id, TransferPaymentDTO dto) {
        return updatePaymentWithBusinessLogic(
            id,
            transferPaymentRepository,
            "Transfer payment not found",
            entity -> {
                PaymentDetailsDTO originalDetails = extractPaymentDetails(entity);
                transferPaymentMapper.updateEntityFromDto(dto, entity);
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails());
            },
            transferPaymentMapper::toResponse
        );
    }

    @Override
    @Transactional
    public CheckPaymentResponseDTO updateCheck(Long id, CheckPaymentDTO dto) {
        return updatePaymentWithBusinessLogic(
            id,
            checkPaymentRepository,
            "Check payment not found",
            entity -> {
                PaymentDetailsDTO originalDetails = extractPaymentDetails(entity);
                checkPaymentMapper.updateEntityFromDto(dto, entity);
                return new PaymentUpdateInfo<>(entity, originalDetails, dto.paymentDetails());
            },
            checkPaymentMapper::toResponse
        );
    }

    /**
     * Clase auxiliar para transportar información de actualización
     */
    private static class PaymentUpdateInfo<T> {
        final T entity;
        final PaymentDetailsDTO originalDetails;
        final PaymentDetailsDTO newDetails;

        PaymentUpdateInfo(T entity, PaymentDetailsDTO originalDetails, PaymentDetailsDTO newDetails) {
            this.entity = entity;
            this.originalDetails = originalDetails;
            this.newDetails = newDetails;
        }
    }

    /**
     * Template method para operaciones de actualización con lógica de negocio
     */
    private <T, R extends PaymentResponseDTO> R updatePaymentWithBusinessLogic(
        Long id,
        JpaRepository<T, Long> repository,
        String errorMessage,
        Function<T, PaymentUpdateInfo<T>> updateFunction,
        Function<T, R> responseMapper
    ) {
        T existing = repository.findById(id)
            .filter(entity -> {
                try {
                    var deletedField = entity.getClass().getDeclaredMethod("getDeleted");
                    deletedField.setAccessible(true);
                    Boolean deleted = (Boolean) deletedField.invoke(entity);
                    return !Boolean.TRUE.equals(deleted);
                } catch (Exception e) {
                    return true;
                }
            })
            .orElseThrow(() -> new PaymentNotFoundException(errorMessage));

        // Ejecutar actualización y obtener información
        PaymentUpdateInfo<T> updateInfo = updateFunction.apply(existing);

        // Solo aplicar lógica de negocio si hay cambios significativos
        if (hasSignificantChanges(updateInfo.originalDetails, updateInfo.newDetails)) {
            // 1. Revertir efectos del pago original
            revertPaymentBusinessLogic(updateInfo.originalDetails);

            // 2. Aplicar efectos del pago actualizado
            executePaymentBusinessLogic(mergePaymentDetails(updateInfo.originalDetails, updateInfo.newDetails));
        }

        T savedEntity = repository.save(updateInfo.entity);
        return responseMapper.apply(savedEntity);
    }

    /**
     * Método responsable de eliminar pagos en efectivo
     * Revierte todos los efectos del pago antes de marcarlo como eliminado
     */
    @Override
    @Transactional
    public void deleteCash(Long id) {
        deletePaymentWithBusinessLogic(id, cashPaymentRepository, "Cash payment not found");
    }

    /**
     * Método responsable de eliminar pagos por transferencia
     * Revierte todos los efectos del pago antes de marcarlo como eliminado
     */
    @Override
    @Transactional
    public void deleteTransfer(Long id) {
        deletePaymentWithBusinessLogic(id, transferPaymentRepository, "Transfer payment not found");
    }

    /**
     * Método responsable de eliminar pagos por cheque
     * Revierte todos los efectos del pago antes de marcarlo como eliminado
     */
    @Override
    @Transactional
    public void deleteCheck(Long id) {
        deletePaymentWithBusinessLogic(id, checkPaymentRepository, "Check payment not found");
    }

    /**
     * Template method para operaciones de eliminación con lógica de negocio
     * Revierte todos los efectos del pago antes de marcarlo como eliminado
     */
    private <T> void deletePaymentWithBusinessLogic(Long id, JpaRepository<T, Long> repository, String errorMessage) {
        T existing = repository.findById(id)
            .filter(entity -> {
                try {
                    var deletedField = entity.getClass().getDeclaredMethod("getDeleted");
                    deletedField.setAccessible(true);
                    Boolean deleted = (Boolean) deletedField.invoke(entity);
                    return !Boolean.TRUE.equals(deleted);
                } catch (Exception e) {
                    return true;
                }
            })
            .orElseThrow(() -> new PaymentNotFoundException(errorMessage));

        // 1. Extraer información del pago antes de eliminarlo
        PaymentDetailsDTO paymentDetails = extractPaymentDetails(existing);

        // 2. Revertir todos los efectos del pago en el sistema
        revertPaymentBusinessLogic(paymentDetails);

        // 3. Marcar como eliminado
        try {
            var setDeletedMethod = existing.getClass().getDeclaredMethod("setDeleted", Boolean.class);
            setDeletedMethod.setAccessible(true);
            setDeletedMethod.invoke(existing, true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set deleted flag", e);
        }

        repository.save(existing);
    }

    /**
     * Extrae PaymentDetailsDTO de cualquier tipo de entidad de pago
     */
    private <T> PaymentDetailsDTO extractPaymentDetails(T paymentEntity) {
        try {
            // Usar reflexión para obtener paymentDetails
            var paymentDetailsField = paymentEntity.getClass().getDeclaredMethod("getPaymentDetails");
            paymentDetailsField.setAccessible(true);
            PaymentDetails details = (PaymentDetails) paymentDetailsField.invoke(paymentEntity);

            // Manejar pagos independientes sin documentos asociados
            List<Long> documentIds = null;
            if (details.getPaidDocuments() != null && !details.getPaidDocuments().isEmpty()) {
                documentIds = details.getPaidDocuments().stream()
                    .map(TransactionalDocument::getId)
                    .toList();
            }

            return new PaymentDetailsDTO(
                details.getPaymentDate(),
                details.getSupplier().getId(),
                details.getAmount(),
                details.getComment(),
                documentIds // Puede ser null para pagos independientes
            );
        } catch (Exception e) {
            throw new RuntimeException("Error extracting payment details", e);
        }
    }

    /**
     * Verifica si hay cambios significativos que requieren actualización de lógica de negocio
     */
    private boolean hasSignificantChanges(PaymentDetailsDTO original, PaymentDetailsDTO updated) {
        // Obtener valores finales para comparación
        Long finalSupplierId = getValueOrOriginal(updated.supplierId(), original.supplierId());
        BigDecimal finalAmount = getValueOrOriginal(updated.amount(), original.amount());
        List<Long> finalPaidDocumentIds = getValueOrOriginal(updated.paidDocumentIds(), original.paidDocumentIds());

        // Cambios en campos que afectan la lógica de negocio
        boolean supplierChanged = !original.supplierId().equals(finalSupplierId);
        boolean amountChanged = !original.amount().equals(finalAmount);
        boolean documentsChanged = !areDocumentListsEqual(original.paidDocumentIds(), finalPaidDocumentIds);

        return supplierChanged || amountChanged || documentsChanged;
    }

    /**
     * Compara dos listas de IDs de documentos, manejando casos null
     */
    private boolean areDocumentListsEqual(List<Long> list1, List<Long> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.isEmpty() && list2.isEmpty()) return true;
        return list1.equals(list2);
    }

    /**
     * Combina detalles originales con actualizaciones (solo campos no-null)
     */
    private PaymentDetailsDTO mergePaymentDetails(PaymentDetailsDTO original, PaymentDetailsDTO updated) {
        return new PaymentDetailsDTO(
            getValueOrOriginal(updated.paymentDate(), original.paymentDate()),
            getValueOrOriginal(updated.supplierId(), original.supplierId()),
            getValueOrOriginal(updated.amount(), original.amount()),
            getValueOrOriginal(updated.comment(), original.comment()),
            getValueOrOriginal(updated.paidDocumentIds(), original.paidDocumentIds())
        );
    }

    /**
     * Retorna el valor actualizado si no es null, sino el original
     */
    private <V> V getValueOrOriginal(V updatedValue, V originalValue) {
        return updatedValue != null ? updatedValue : originalValue;
    }

    @Override
    @Transactional(readOnly = true)
    public CashPaymentResponseDTO getCash(Long id) {
        return getPayment(id, cashPaymentRepository, "Cash payment not found", cashPaymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferPaymentResponseDTO getTransfer(Long id) {
        return getPayment(id, transferPaymentRepository, "Transfer payment not found", transferPaymentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckPaymentResponseDTO getCheck(Long id) {
        return getPayment(id, checkPaymentRepository, "Check payment not found", checkPaymentMapper::toResponse);
    }

    /**
     * Template method para operaciones de consulta por ID
     * Centraliza la lógica de búsqueda y mapeo
     */
    private <T, R extends PaymentResponseDTO> R getPayment(
        Long id,
        JpaRepository<T, Long> repository,
        String errorMessage,
        Function<T, R> responseMapper
    ) {
        T existing = repository.findById(id)
            .filter(entity -> {
                try {
                    var deletedField = entity.getClass().getDeclaredMethod("getDeleted");
                    deletedField.setAccessible(true);
                    Boolean deleted = (Boolean) deletedField.invoke(entity);
                    return !Boolean.TRUE.equals(deleted);
                } catch (Exception e) {
                    return true;
                }
            })
            .orElseThrow(() -> new PaymentNotFoundException(errorMessage));

        return responseMapper.apply(existing);
    }
}
