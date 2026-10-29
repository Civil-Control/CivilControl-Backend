package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.contracts.Certification;
import PSG.backEnd.model.entity.sales.SalesDocument;
import PSG.backEnd.model.entity.sales.SalesItemDetail;
import PSG.backEnd.repository.CertificationRepository;
import PSG.backEnd.repository.SalesDocumentRepository;
import PSG.backEnd.service.port.IClientLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Recomputes the totals of a SalesDocument from its items and its linked certifications,
 * mirroring {@link DocumentTotalRecalculator} on the purchases side (transactional-document
 * items + linked repairs). Extracted so CertificationService can trigger it on link/unlink
 * without depending on SalesDocumentService.
 */
@Service
public class SalesDocumentTotalRecalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SalesDocumentRepository salesDocumentRepository;
    private final CertificationRepository certificationRepository;
    private IClientLedgerService clientLedgerService;

    public SalesDocumentTotalRecalculator(SalesDocumentRepository salesDocumentRepository,
                                          CertificationRepository certificationRepository) {
        this.salesDocumentRepository = salesDocumentRepository;
        this.certificationRepository = certificationRepository;
    }

    @Autowired
    public void setClientLedgerService(@Lazy IClientLedgerService clientLedgerService) {
        this.clientLedgerService = clientLedgerService;
    }

    /** Recalculates, persists and re-syncs the ledger movement of the given sales document. */
    @Transactional
    public void recalculate(Long salesDocumentId) {
        if (salesDocumentId == null) return;
        SalesDocument document = salesDocumentRepository.findByIdAndDeletedFalse(salesDocumentId).orElse(null);
        if (document == null) return;
        applyTotals(document);
        SalesDocument saved = salesDocumentRepository.save(document);
        if (clientLedgerService != null) {
            clientLedgerService.syncDocumentMovement(saved);
        }
    }

    /** Computes netTotal/ivaTotal/ivaExemptTotal/total onto the entity without saving it. */
    public void applyTotals(SalesDocument document) {
        BigDecimal[] acc = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};

        for (SalesItemDetail item : document.getItems()) {
            if (item.getUnitAmount() == null || item.getQuantity() == null) continue;
            accumulate(acc, item.getUnitAmount().multiply(BigDecimal.valueOf(item.getQuantity())), item.getIvaPercentage());
        }

        if (document.getId() != null) {
            for (Certification cert : certificationRepository.findBySalesDocumentIdAndDeletedFalse(document.getId())) {
                accumulate(acc, cert.getCertifiedAmount(), cert.getIvaPercentage());
            }
        }

        BigDecimal netTotal = acc[0].setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaTotal = acc[1].setScale(2, RoundingMode.HALF_UP);
        BigDecimal ivaExemptTotal = acc[2].setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountPct = document.getDiscountPercentage() != null ? document.getDiscountPercentage() : BigDecimal.ZERO;
        BigDecimal otherTaxes = document.getOtherTaxes() != null ? document.getOtherTaxes() : BigDecimal.ZERO;
        document.setDiscountPercentage(discountPct);
        document.setOtherTaxes(otherTaxes);

        BigDecimal subtotal = netTotal.add(ivaTotal).add(ivaExemptTotal);
        BigDecimal discountFactor = BigDecimal.ONE.subtract(discountPct.divide(HUNDRED, 10, RoundingMode.HALF_UP));
        BigDecimal total = subtotal.multiply(discountFactor).add(otherTaxes).setScale(2, RoundingMode.HALF_UP);

        document.setNetTotal(netTotal);
        document.setIvaTotal(ivaTotal);
        document.setIvaExemptTotal(ivaExemptTotal);
        document.setTotal(total);
    }

    private static void accumulate(BigDecimal[] acc, BigDecimal rawSubtotal, BigDecimal ivaPct) {
        if (rawSubtotal == null) return;
        if (ivaPct != null && ivaPct.compareTo(BigDecimal.ZERO) > 0) {
            acc[0] = acc[0].add(rawSubtotal.setScale(2, RoundingMode.HALF_UP));
            acc[1] = acc[1].add(rawSubtotal.multiply(ivaPct)
                    .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            acc[2] = acc[2].add(rawSubtotal.setScale(2, RoundingMode.HALF_UP));
        }
    }
}
