package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "contact_info")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ContactInfo extends TenantEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_name", nullable = false, columnDefinition = "VARCHAR(100)")
    private String referenceName;

    @ElementCollection
    @CollectionTable(name = "contact_info_emails", joinColumns = @JoinColumn(name = "contact_info_id"))
    @Column(name = "email", columnDefinition = "VARCHAR(100)")
    private List<String> email;

    @ElementCollection
    @CollectionTable(name = "contact_info_phones", joinColumns = @JoinColumn(name = "contact_info_id"))
    @Column(name = "phone_number", columnDefinition = "VARCHAR(30)")
    private List<String> phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
}
