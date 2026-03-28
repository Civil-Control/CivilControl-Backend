package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Item;
import PSG.backEnd.model.enums.ItemType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT DISTINCT i FROM Item i LEFT JOIN i.itemTypes t " +
            "WHERE (:name IS NULL OR LOWER(CAST(i.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:description IS NULL OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:description AS string), '%'))) " +
           "AND (:search IS NULL OR (LOWER(CAST(i.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "     OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))) " +
           "AND (:itemType IS NULL OR :itemType MEMBER OF i.itemTypes)")
    Page<Item> findAllWithFilters(
            @Param("name") String name,
            @Param("description") String description,
            @Param("search") String search,
            @Param("itemType") ItemType itemType,
            Pageable pageable
    );

    /**
     * Find all items that have the given type (COMPRA or VENTA).
     */
    @Query("SELECT i FROM Item i WHERE :itemType MEMBER OF i.itemTypes")
    List<Item> findByItemType(@Param("itemType") ItemType itemType);
}
