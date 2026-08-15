package com.orcafin.service;

import com.orcafin.entity.Vault;
import com.orcafin.entity.VaultMovement;
import com.orcafin.entity.VaultMovementType;
import com.orcafin.repository.VaultMovementRepository;
import com.orcafin.repository.VaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Credita diariamente o rendimento composto de cada caixinha, a partir da taxa anual
 * informada pelo usuário: dailyRate = (1 + annualRate/100)^(1/365) - 1.
 */
@Component
@RequiredArgsConstructor
public class VaultYieldScheduler {

    private final VaultRepository vaultRepository;
    private final VaultMovementRepository vaultMovementRepository;

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void applyDailyYield() {
        for (Vault vault : vaultRepository.findAll()) {
            if (vault.getAnnualRate().compareTo(BigDecimal.ZERO) <= 0
                    || vault.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal dailyRate = dailyRate(vault.getAnnualRate());
            BigDecimal yieldAmount = vault.getBalance().multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
            if (yieldAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            vault.setBalance(vault.getBalance().add(yieldAmount));
            vaultRepository.save(vault);

            VaultMovement movement = new VaultMovement();
            movement.setVault(vault);
            movement.setType(VaultMovementType.RENDIMENTO);
            movement.setAmount(yieldAmount);
            movement.setDate(LocalDate.now());
            vaultMovementRepository.save(movement);
        }
    }

    private BigDecimal dailyRate(BigDecimal annualRatePercent) {
        double annualRate = annualRatePercent.doubleValue() / 100.0;
        double daily = Math.pow(1 + annualRate, 1.0 / 365.0) - 1;
        return BigDecimal.valueOf(daily).round(new MathContext(10));
    }
}
