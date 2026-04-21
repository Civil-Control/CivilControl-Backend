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

        // Running totals (per-record IVA, mirroring ItemDetail logic).
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        BigDecimal exempt = BigDecimal.ZERO;

        // --- ItemDetails ---
        for (ItemDetail item : document.getItems()) {
            if (item.getUnitAmount() == null || item.getQuantity() == null) continue;
            BigDecimal subtotal = item.getUnitAmount().multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal[] split = splitNetIva(subtotal, item.getIvaPercentage());
            net    = net.add(split[0]);
            iva    = iva.add(split[1]);
            exempt = exempt.add(split[2]);
        }

        // --- FuelLoads ---
        for (FuelLoad fl : fuelLoadRepository.findByTransactionalDocumentId(documentId)) {
            BigDecimal[] split = splitNetIva(fl.getTotalAmount(), fl.getIvaPercentage());
            net    = net.add(split[0]);
            iva    = iva.add(split[1]);
            exempt = exempt.add(split[2]);
        }

        // --- RepairItems ---
        for (RepairItem ri : repairItemRepository.findByTransactionalDocumentId(documentId)) {
            BigDecimal[] split = splitNetIva(ri.getAmount(), ri.getIvaPercentage());
            net    = net.add(split[0]);
            iva    = iva.add(split[1]);
            exempt = exempt.add(split[2]);
        }

        // --- SalaryPayments ---
        for (SalaryPayment sp : salaryPaymentRepository.findByTransactionalDocumentId(documentId)) {
            BigDecimal[] split = splitNetIva(sp.getAmount(), sp.getIvaPercentage());
            net    = net.add(split[0]);
            iva    = iva.add(split[1]);
            exempt = exempt.add(split[2]);
        }

        // --- StockPurchases ---
        for (StockPurchase stp : stockPurchaseRepository.findByTransactionalDocumentId(documentId)) {
            BigDecimal[] split = splitNetIva(stp.getTotalAmount(), stp.getIvaPercentage());
            net    = net.add(split[0]);
            iva    = iva.add(split[1]);
            exempt = exempt.add(split[2]);
        }

        BigDecimal netTotal        = net.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaTotal        = iva.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaExemptTotal  = exempt.setScale(2, RoundingMode.HALF_UP);
        BigDecimal otherTaxes      = document.getOtherTaxes() != null ? document.getOtherTaxes() : BigDecimal.ZERO;
        BigDecimal total           = netTotal.add(ivaTotal).add(ivaExemptTotal).add(otherTaxes)
                .setScale(2, RoundingMode.HALF_UP);

        document.setNetTotal(netTotal);
        document.setIvaTotal(ivaTotal);
        document.setIvaExemptTotal(ivaExemptTotal);
        document.setTotal(total);

        transactionalDocumentRepository.save(document);
    }

    /**
     * Splits an amount into [net, iva, exempt] according to the IVA percentage.
     * - If the amount is null, returns zeros.
     * - If the IVA percentage is null or zero, the entire amount counts as exempt.
     * - Otherwise the amount is treated as net and IVA is computed on top.
     */
    private static BigDecimal[] splitNetIva(BigDecimal amount, BigDecimal ivaPercentage) {
        if (amount == null) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        }
        if (ivaPercentage == null || ivaPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, amount};
        }
        BigDecimal factor = ivaPercentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal ivaPart = amount.multiply(factor);
        return new BigDecimal[]{amount, ivaPart, BigDecimal.ZERO};
    }
}
