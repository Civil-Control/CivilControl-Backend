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

    @Column(columnDefinition = "VARCHAR(200)")
    private String street;

    @Column
    private Integer number;

    @Column(columnDefinition = "VARCHAR(100)")
    private String city;

    @Column(columnDefinition = "VARCHAR(100)")
    private String state;

    @Column(columnDefinition = "VARCHAR(100)")
    private String country;

    @Column(columnDefinition = "VARCHAR(20)")
    private String zipCode;
}
