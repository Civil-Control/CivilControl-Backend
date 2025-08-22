package PSG.backEnd.model.entity.payment;

import PSG.backEnd.model.entity.TransactionalDocument;
import PSG.backEnd.model.entity.Supplier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @ManyToMany
    @JoinTable(name = "payment_details_paid_documents",
            joinColumns = @JoinColumn(name = "payment_details_id"),
            inverseJoinColumns = @JoinColumn(name = "transactional_document_id"))
    private List<TransactionalDocument> paidDocuments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String comment;

    @OneToOne(mappedBy = "paymentDetails")
    private CashPayment cashPayment;

    @OneToOne(mappedBy = "paymentDetails")
    private TransferPayment transferPayment;

    @OneToOne(mappedBy = "paymentDetails")
    private CheckPayment checkPayment;
}
