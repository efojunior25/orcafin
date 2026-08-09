package com.orcafin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = true)
    @JoinColumn(name = "account_id", nullable = true)
    private Account account;

    @ManyToOne(optional = true)
    @JoinColumn(name = "credit_card_id", nullable = true)
    private CreditCard creditCard;

    @ManyToOne(optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    @ManyToOne(optional = true)
    @JoinColumn(name = "destination_account_id", nullable = true)
    private Account destinationAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_group")
    private TransactionGroup group;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean isRecurring;

    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency recurrenceFrequency;

    private LocalDate recurrenceEndDate;

    /** Agrupa as parcelas de uma mesma compra parcelada no cartão (null = não parcelada). */
    private UUID installmentGroupId;

    private Integer installmentNumber;

    private Integer installmentTotal;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
