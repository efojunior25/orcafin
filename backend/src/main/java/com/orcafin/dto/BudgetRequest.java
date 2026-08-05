package com.orcafin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class BudgetRequest {

    private UUID categoryId;

    @NotNull
    @Positive
    private BigDecimal limitAmount;
}
