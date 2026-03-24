package PSG.backEnd.model.dto.contracts;

import java.math.BigDecimal;

/**
 * Projection used by the JPQL constructor expression in CertificationRepository.
 * Holds raw sums needed to build WorkContractStatsDTO.
 */
public class CertificationStatsProjection {

    private final BigDecimal totalCertified;
    private final BigDecimal totalInvoiced;
    private final BigDecimal totalCollected;

    public CertificationStatsProjection(BigDecimal totalCertified,
                                        BigDecimal totalInvoiced,
                                        BigDecimal totalCollected) {
        this.totalCertified = totalCertified;
        this.totalInvoiced  = totalInvoiced;
        this.totalCollected = totalCollected;
    }

    public BigDecimal getTotalCertified()  { return totalCertified; }
    public BigDecimal getTotalInvoiced()   { return totalInvoiced; }
    public BigDecimal getTotalCollected()  { return totalCollected; }
}
