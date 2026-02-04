package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Building;
import PSG.backEnd.model.enums.BuildingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    Optional<Building> findByIdAndDeletedFalse(Long id);

    Optional<Building> findByCode(String code);

    Optional<Building> findByCodeAndDeletedFalse(String code);

    Optional<Building> findByCodeAndDeletedTrue(String code);

    boolean existsByCodeAndDeletedFalse(String code);

    boolean existsByIdAndDeletedFalse(Long id);

    Page<Building> findByDeletedFalse(Pageable pageable);

    Page<Building> findByActiveAndDeletedFalse(Boolean active, Pageable pageable);

    Page<Building> findByBuildingTypeAndDeletedFalse(BuildingType buildingType, Pageable pageable);

    @Query("SELECT b FROM Building b " +
            "WHERE b.deleted = false " +
            "AND (:name IS NULL OR LOWER(CAST(b.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
            "AND (:code IS NULL OR LOWER(CAST(b.code AS string)) LIKE LOWER(CONCAT('%', CAST(:code AS string), '%'))) " +
            "AND (:buildingType IS NULL OR b.buildingType = :buildingType) " +
            "AND (CAST(:projectAreaId AS long) IS NULL OR b.projectArea.id = :projectAreaId) " +
            "AND (:active IS NULL OR b.active = :active)")
    Page<Building> findAllWithFilters(
            @Param("name") String name,
            @Param("code") String code,
            @Param("buildingType") BuildingType buildingType,
            @Param("projectAreaId") Long projectAreaId,
            @Param("active") Boolean active,
            Pageable pageable
    );
}

