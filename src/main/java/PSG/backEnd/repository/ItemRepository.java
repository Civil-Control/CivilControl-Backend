package PSG.backEnd.repository;

import PSG.backEnd.model.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Page<Item> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT i FROM Item i " +
            "WHERE (:name IS NULL OR LOWER(CAST(i.name AS string)) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
           "AND (:description IS NULL OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:description AS string), '%'))) " +
           "AND (:search IS NULL OR (LOWER(CAST(i.name AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "     OR LOWER(CAST(i.description AS string)) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
    Page<Item> findAllWithFilters(
            @Param("name") String name,
            @Param("description") String description,
            @Param("search") String search,
            Pageable pageable
    );
}
