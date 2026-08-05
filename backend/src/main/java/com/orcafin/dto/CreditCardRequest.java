package com.orcafin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreditCardRequest {

    @NotBlank
    private String name;

    @NotNull
    private BigDecimal creditLimit;

    @NotNull
    @Min(1)
    @Max(28)
    private Integer closingDay;

    @NotNull
    @Min(1)
    @Max(28)
    private Integer dueDay;
}
