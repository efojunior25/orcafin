package com.orcafin.dto;

import com.orcafin.entity.CreditCard;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class CreditCardResponse {
    private final UUID id;
    private final String name;
    private final BigDecimal creditLimit;
    private final int closingDay;
    private final int dueDay;
    private final BigDecimal currentInvoiceTotal;
    private final BigDecimal usedLimit;
    private final Instant createdAt;

    public CreditCardResponse(CreditCard creditCard, BigDecimal currentInvoiceTotal, BigDecimal usedLimit) {
        this.id = creditCard.getId();
        this.name = creditCard.getName();
        this.creditLimit = creditCard.getCreditLimit();
        this.closingDay = creditCard.getClosingDay();
        this.dueDay = creditCard.getDueDay();
        this.currentInvoiceTotal = currentInvoiceTotal;
        this.usedLimit = usedLimit;
        this.createdAt = creditCard.getCreatedAt();
    }
}
