package PSG.backEnd.repository;

import PSG.backEnd.model.entity.CreditNoteApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CreditNoteApplicationRepository extends JpaRepository<CreditNoteApplication, Long> {

    List<CreditNoteApplication> findByCreditNote_Id(Long creditNoteId);

    List<CreditNoteApplication> findByInvoice_Id(Long invoiceId);

    /** Sum of credit applied to an invoice, excluding rows that belong to deleted credit notes. */
    @Query("SELECT COALESCE(SUM(a.amountApplied), 0) FROM CreditNoteApplication a " +
           "WHERE a.invoice.id = :invoiceId AND a.creditNote.deleted = false")
    BigDecimal sumAppliedToInvoice(@Param("invoiceId") Long invoiceId);

    void deleteByCreditNote_Id(Long creditNoteId);

    boolean existsByCreditNote_IdAndInvoice_Id(Long creditNoteId, Long invoiceId);

    /** Sum of credit applied FROM a specific credit note across all its applications. */
    @Query("SELECT COALESCE(SUM(a.amountApplied), 0) FROM CreditNoteApplication a " +
           "WHERE a.creditNote.id = :creditNoteId AND a.creditNote.deleted = false")
    BigDecimal sumAppliedByCreditNote(@Param("creditNoteId") Long creditNoteId);
}
