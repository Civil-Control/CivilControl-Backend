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
public class ContactInfo {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column
    private List<String> email;

    @Column
    private List<String> phoneNumber;
}
