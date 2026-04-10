package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.ItemDetail;
import PSG.backEnd.model.entity.StockPurchase;
import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.employee.SalaryPayment;
import PSG.backEnd.model.entity.gasStation.FuelLoad;
import PSG.backEnd.model.entity.vehicle.RepairItem;
import PSG.backEnd.repository.FuelLoadRepository;
import PSG.backEnd.repository.RepairItemRepository;
import PSG.backEnd.repository.SalaryPaymentRepository;
import PSG.backEnd.repository.StockPurchaseRepository;
import PSG.backEnd.repository.TransactionalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Recalculates the totals of a TransactionalDocument based on its ItemDetails
 * and all linked records (FuelLoad, Repair, SalaryPayment, StockPurchase).
 * Extracted to its own service to avoid circular dependencies.
 */
@Service
@RequiredArgsConstructor
public class DocumentTotalRecalculator {

    private final TransactionalDocumentRepository transactionalDocumentRepository;
    private final FuelLoadRepository fuelLoadRepository;
    private final RepairItemRepository repairItemRepository;
    private final SalaryPaymentRepository salaryPaymentRepository;
    private final StockPurchaseRepository stockPurchaseRepository;

    @Transactional
    public void recalculateDocumentTotals(Long documentId) {
        if (documentId == null) return;

        TransactionalDocument document = transactionalDocumentRepository.findByIdAndDeletedFalse(documentId)
                .orElse(null);
        if (document == null) return;

        BigDecimal IVA_FACTOR = new BigDecimal("21")
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // --- Sum ItemDetails ---
        BigDecimal itemsNet = BigDecimal.ZERO;
        BigDecimal itemsIva = BigDecimal.ZERO;
        BigDecimal itemsExempt = BigDecimal.ZERO;

        for (ItemDetail item : document.getItems()) {
            BigDecimal subtotal = item.getUnitAmount().multiply(BigDecimal.valueOf(item.getQuantity()));
            if (item.getIvaPercentage() != null && item.getIvaPercentage().compareTo(BigDecimal.ZERO) > 0) {
                itemsNet = itemsNet.add(subtotal);
                BigDecimal iva = subtotal.multiply(
                        item.getIvaPercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                itemsIva = itemsIva.add(iva);
            } else {
                itemsExempt = itemsExempt.add(subtotal);
            }
        }

        // --- Sum linked records (all treated with 21% IVA) ---
        BigDecimal linkedNet = BigDecimal.ZERO;

        for (FuelLoad fl : fuelLoadRepository.findByTransactionalDocumentId(documentId)) {
            if (fl.getTotalAmount() != null) linkedNet = linkedNet.add(fl.getTotalAmount());
        }
        for (RepairItem ri : repairItemRepository.findByTransactionalDocumentId(documentId)) {
            if (ri.getAmount() != null) linkedNet = linkedNet.add(ri.getAmount());
        }
        for (SalaryPayment sp : salaryPaymentRepository.findByTransactionalDocumentId(documentId)) {
            if (sp.getAmount() != null) linkedNet = linkedNet.add(sp.getAmount());
        }
        for (StockPurchase stp : stockPurchaseRepository.findByTransactionalDocumentId(documentId)) {
            if (stp.getTotalAmount() != null) linkedNet = linkedNet.add(stp.getTotalAmount());
        }

        BigDecimal linkedIva = linkedNet.multiply(IVA_FACTOR);

        // --- Compute final totals ---
        BigDecimal netTotal = itemsNet.add(linkedNet).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaTotal = itemsIva.add(linkedIva).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaExemptTotal = itemsExempt.setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherTaxes = document.getOtherTaxes() != null ? document.getOtherTaxes() : BigDecimal.ZERO;
        BigDecimal total = netTotal.add(ivaTotal).add(ivaExemptTotal).add(otherTaxes)
                .setScale(2, RoundingMode.HALF_UP);

        document.setNetTotal(netTotal);
        document.setIvaTotal(ivaTotal);
        document.setIvaExemptTotal(ivaExemptTotal);
        document.setTotal(total);

        transactionalDocumentRepository.save(document);
    }
}
