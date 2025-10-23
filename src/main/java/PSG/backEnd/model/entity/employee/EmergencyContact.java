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

    @Column(name = "emergency_contact_name")
    private String name;

    @Column(name = "emergency_contact_phone")
    private String phoneNumber;

    @Column(name = "emergency_contact_relationship")
    private String relationship;
}
