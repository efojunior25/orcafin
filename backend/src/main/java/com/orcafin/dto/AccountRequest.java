package com.orcafin.dto;

import com.orcafin.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountRequest {

    @NotBlank
    private String name;

    @NotNull
    private AccountType type;

    private BigDecimal balance;
}
