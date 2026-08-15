package com.orcafin.dto;

import com.orcafin.entity.PrepaidCard;
import com.orcafin.entity.PrepaidCardType;
import com.orcafin.entity.TransitSubtype;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class PrepaidCardResponse {
    private final UUID id;
    private final String name;
    private final PrepaidCardType type;
    private final TransitSubtype subtype;
    private final BigDecimal balance;
    private final Integer rechargeDay;
    private final BigDecimal rechargeAmount;
    private final Instant createdAt;

    public PrepaidCardResponse(PrepaidCard prepaidCard) {
        this.id = prepaidCard.getId();
        this.name = prepaidCard.getName();
        this.type = prepaidCard.getType();
        this.subtype = prepaidCard.getSubtype();
        this.balance = prepaidCard.getBalance();
        this.rechargeDay = prepaidCard.getRechargeDay();
        this.rechargeAmount = prepaidCard.getRechargeAmount();
        this.createdAt = prepaidCard.getCreatedAt();
    }
}
