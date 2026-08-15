package com.orcafin.dto;

import com.orcafin.entity.VaultMovement;
import com.orcafin.entity.VaultMovementType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class VaultMovementResponse {
    private final UUID id;
    private final VaultMovementType type;
    private final BigDecimal amount;
    private final LocalDate date;

    public VaultMovementResponse(VaultMovement movement) {
        this.id = movement.getId();
        this.type = movement.getType();
        this.amount = movement.getAmount();
        this.date = movement.getDate();
    }
}
