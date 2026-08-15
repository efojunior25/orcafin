package com.orcafin.dto;

import com.orcafin.entity.InvestmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvestmentRequest {

    @NotBlank
    private String name;

    @NotNull
    private InvestmentType type;

    @NotNull
    private BigDecimal investedAmount;

    @NotNull
    private BigDecimal currentValue;

    private String notes;
}
