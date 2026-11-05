package PSG.backEnd.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embedded entity representing a user's default geographic location.
 * Used to pre-fill address fields across forms (city, state, country, zip code).
 * All fields are nullable so existing users are unaffected.
 */
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserLocation {

    @Column(name = "location_city", columnDefinition = "VARCHAR(100)")
    private String city;

    @Column(name = "location_state", columnDefinition = "VARCHAR(100)")
    private String state;

    @Column(name = "location_country", columnDefinition = "VARCHAR(100)")
    private String country;

    @Column(name = "location_zip_code", columnDefinition = "VARCHAR(20)")
    private String zipCode;
}
