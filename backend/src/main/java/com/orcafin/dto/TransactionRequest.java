package com.orcafin.dto;

import com.orcafin.entity.PaymentMethod;
import com.orcafin.entity.RecurrenceFrequency;
import com.orcafin.entity.TransactionGroup;
import com.orcafin.entity.TransactionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class TransactionRequest {

    private UUID accountId;

    private UUID creditCardId;

    private UUID categoryId;

    private UUID destinationAccountId;

    @NotNull
    private TransactionType type;

    private TransactionGroup group;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String description;

    @NotNull
    private LocalDate date;

    @NotNull
    private PaymentMethod paymentMethod;

    private boolean isRecurring;

    private RecurrenceFrequency recurrenceFrequency;

    private LocalDate recurrenceEndDate;

    /** Número de parcelas (cartão de crédito apenas). Null ou 1 = sem parcelamento. */
    @Min(1)
    @Max(48)
    private Integer installments;
}
