package com.orcafin.dto;

import com.orcafin.entity.Budget;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class BudgetResponse {
    private final UUID id;
    private final UUID categoryId;
    private final String categoryName;
    private final BigDecimal limitAmount;
    private final BigDecimal spentAmount;
    private final double percentage;

    public BudgetResponse(Budget budget, BigDecimal spentAmount) {
        this.id = budget.getId();
        this.categoryId = budget.getCategory() != null ? budget.getCategory().getId() : null;
        this.categoryName = budget.getCategory() != null ? budget.getCategory().getName() : "Geral";
        this.limitAmount = budget.getLimitAmount();
        this.spentAmount = spentAmount;
        this.percentage = budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                ? spentAmount.divide(budget.getLimitAmount(), 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100
                : 0;
    }
}
