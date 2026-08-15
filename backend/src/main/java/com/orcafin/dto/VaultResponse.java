package com.orcafin.dto;

import com.orcafin.entity.Vault;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class VaultResponse {
    private final UUID id;
    private final String name;
    private final BigDecimal balance;
    private final BigDecimal annualRate;
    private final Instant createdAt;

    public VaultResponse(Vault vault) {
        this.id = vault.getId();
        this.name = vault.getName();
        this.balance = vault.getBalance();
        this.annualRate = vault.getAnnualRate();
        this.createdAt = vault.getCreatedAt();
    }
}
