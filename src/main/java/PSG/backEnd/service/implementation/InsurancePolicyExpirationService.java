package PSG.backEnd.service.implementation;

import PSG.backEnd.repository.InsurancePolicyRepository;
import PSG.backEnd.repository.TenantRepository;
import PSG.backEnd.service.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Marks as VENCIDO every ACTIVO insurance policy whose end date has already passed.
 * Runs at startup (catch-up) and every day just after midnight, Argentina time.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsurancePolicyExpirationService {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final InsurancePolicyRepository insurancePolicyRepository;
    private final TenantRepository tenantRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void expireOnStartup() {
        expireOverduePolicies();
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "America/Argentina/Buenos_Aires")
    public void expireDaily() {
        expireOverduePolicies();
    }

    public int expireOverduePolicies() {
        LocalDate today = LocalDate.now(ZONE);
        int total = 0;
        for (var tenant : tenantRepository.findAllByDeletedFalseAndActiveTrue()) {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                total += insurancePolicyRepository.expireOverdueActivePolicies(today, tenant.getId());
            } catch (Exception e) {
                log.error("InsurancePolicyExpiration: error processing tenant {}: {}", tenant.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        log.info("InsurancePolicyExpiration: {} policies marked as VENCIDO", total);
        return total;
    }
}
