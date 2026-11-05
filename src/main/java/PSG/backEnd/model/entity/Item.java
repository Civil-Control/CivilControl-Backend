package PSG.backEnd.model.entity;

import PSG.backEnd.model.enums.ItemType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "items", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "name"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Item extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * The usage types of this item (COMPRA, VENTA, or both).
     * Stored in a separate join table {@code item_types}.
     */
    @ElementCollection(targetClass = ItemType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "item_types", joinColumns = @JoinColumn(name = "item_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    @Builder.Default
    private Set<ItemType> itemTypes = new HashSet<>();

    @Column(nullable = false)
    private boolean deleted;
}
