package PSG.backEnd.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Address {

    @Column(nullable = false, columnDefinition = "VARCHAR(200)")
    private String street;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false, columnDefinition = "VARCHAR(100)")
    private String city;

    @Column(nullable = false, columnDefinition = "VARCHAR(100)")
    private String state;

    @Column(nullable = false, columnDefinition = "VARCHAR(100)")
    private String country;

    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private String zipCode;
}
