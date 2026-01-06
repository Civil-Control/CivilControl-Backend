package PSG.backEnd.model.entity.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EmergencyContact {

    @Column(name = "emergency_contact_name", columnDefinition = "VARCHAR(100)")
    private String name;

    @Column(name = "emergency_contact_phone", columnDefinition = "VARCHAR(30)")
    private String phoneNumber;

    @Column(name = "emergency_contact_relationship", columnDefinition = "VARCHAR(50)")
    private String relationship;
}
