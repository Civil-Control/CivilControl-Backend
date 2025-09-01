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
            "WHERE (:name IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:description IS NULL OR LOWER(i.description) LIKE LOWER(CONCAT('%', :description, '%')))")
    Page<Item> findAllWithFilters(
            @Param("name") String name,
            @Param("description") String description,
            Pageable pageable
    );
}
