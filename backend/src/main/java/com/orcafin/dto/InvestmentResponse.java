package com.orcafin.dto;

import com.orcafin.entity.Investment;
import com.orcafin.entity.InvestmentType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class InvestmentResponse {
    private final UUID id;
    private final String name;
    private final InvestmentType type;
    private final BigDecimal investedAmount;
    private final BigDecimal currentValue;
    private final String notes;
    private final Instant createdAt;
    private final Instant updatedAt;

    public InvestmentResponse(Investment investment) {
        this.id = investment.getId();
        this.name = investment.getName();
        this.type = investment.getType();
        this.investedAmount = investment.getInvestedAmount();
        this.currentValue = investment.getCurrentValue();
        this.notes = investment.getNotes();
        this.createdAt = investment.getCreatedAt();
        this.updatedAt = investment.getUpdatedAt();
    }
}
