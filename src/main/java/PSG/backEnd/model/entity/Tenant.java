package PSG.backEnd.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cuit"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, columnDefinition = "VARCHAR(150)")
    private String name;

    @Column(nullable = false, unique = true, length = 13, columnDefinition = "VARCHAR(13)")
    private String cuit;

    @Column(name = "legal_name", length = 200, columnDefinition = "VARCHAR(200)")
    private String legalName;

    @Embedded
    private Address address;

    @Column(length = 30, columnDefinition = "VARCHAR(30)")
    private String phone;

    @Column(length = 100, columnDefinition = "VARCHAR(100)")
    private String email;

    @Column(name = "logo_url", length = 500, columnDefinition = "VARCHAR(500)")
    private String logoUrl;

    @Column(name = "founded_date")
    private LocalDate foundedDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "webhook_token", unique = true, nullable = false, length = 36)
    private String webhookToken;

    @PrePersist
    private void generateWebhookToken() {
        if (this.webhookToken == null) {
            this.webhookToken = UUID.randomUUID().toString();
        }
    }
}
