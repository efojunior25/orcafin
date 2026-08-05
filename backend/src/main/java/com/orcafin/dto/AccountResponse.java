package com.orcafin.dto;

import com.orcafin.entity.Account;
import com.orcafin.entity.AccountType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class AccountResponse {
    private final UUID id;
    private final String name;
    private final AccountType type;
    private final BigDecimal balance;
    private final Instant createdAt;

    public AccountResponse(Account account) {
        this.id = account.getId();
        this.name = account.getName();
        this.type = account.getType();
        this.balance = account.getBalance();
        this.createdAt = account.getCreatedAt();
    }
}
