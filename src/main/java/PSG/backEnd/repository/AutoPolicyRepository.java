package PSG.backEnd.repository;

import PSG.backEnd.model.entity.insurance.AutoPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoPolicyRepository extends JpaRepository<AutoPolicy, Long> {

    List<AutoPolicy> findAllByOrderByIdDesc();
    Optional<AutoPolicy> findByIdAndDeletedFalse(Long id);
    Optional<AutoPolicy> findByInsurancePolicyId(Long insurancePolicyId);
    boolean existsByIdAndDeletedFalse(Long id);
    boolean existsByInsurancePolicyId(Long insurancePolicyId);

    @Query("SELECT ap FROM AutoPolicy ap " +
            "JOIN ap.insurancePolicy ip " +
            "WHERE (:insurancePolicyId IS NULL OR ap.insurancePolicy.id = :insurancePolicyId) " +
            "AND (:policyNumber IS NULL OR LOWER(ip.policyNumber) LIKE LOWER(CONCAT('%', :policyNumber, '%'))) " +
            "ORDER BY ap.id DESC")
    Page<AutoPolicy> findAllWithFilters(
            @Param("insurancePolicyId") Long insurancePolicyId,
            @Param("policyNumber") String policyNumber,
            Pageable pageable
    );
}
