package com.orcafin.dto;

import com.orcafin.entity.PaymentMethod;
import com.orcafin.entity.RecurrenceFrequency;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionGroup;
import com.orcafin.entity.TransactionType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
public class TransactionResponse {
    private final UUID id;
    private final UUID accountId;
    private final String accountName;
    private final UUID creditCardId;
    private final String creditCardName;
    private final UUID categoryId;
    private final String categoryName;
    private final UUID destinationAccountId;
    private final String destinationAccountName;
    private final TransactionType type;
    private final TransactionGroup group;
    private final BigDecimal amount;
    private final String description;
    private final LocalDate date;
    private final PaymentMethod paymentMethod;
    private final boolean isRecurring;
    private final RecurrenceFrequency recurrenceFrequency;
    private final LocalDate recurrenceEndDate;
    private final Instant createdAt;

    public TransactionResponse(Transaction transaction) {
        this.id = transaction.getId();
        this.accountId = transaction.getAccount() != null ? transaction.getAccount().getId() : null;
        this.accountName = transaction.getAccount() != null ? transaction.getAccount().getName() : null;
        this.creditCardId = transaction.getCreditCard() != null ? transaction.getCreditCard().getId() : null;
        this.creditCardName = transaction.getCreditCard() != null ? transaction.getCreditCard().getName() : null;
        this.categoryId = transaction.getCategory() != null ? transaction.getCategory().getId() : null;
        this.categoryName = transaction.getCategory() != null ? transaction.getCategory().getName() : null;
        this.destinationAccountId = transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getId() : null;
        this.destinationAccountName = transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getName() : null;
        this.type = transaction.getType();
        this.group = transaction.getGroup();
        this.amount = transaction.getAmount();
        this.description = transaction.getDescription();
        this.date = transaction.getDate();
        this.paymentMethod = transaction.getPaymentMethod();
        this.isRecurring = transaction.isRecurring();
        this.recurrenceFrequency = transaction.getRecurrenceFrequency();
        this.recurrenceEndDate = transaction.getRecurrenceEndDate();
        this.createdAt = transaction.getCreatedAt();
    }
}
