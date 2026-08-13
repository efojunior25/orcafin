package com.orcafin.dto;

import com.orcafin.entity.SuggestionType;
import com.orcafin.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Uma sugestão de correção gerada comparando um extrato importado com o que já
 * está lançado no app. Nunca é aplicada sozinha — o usuário revisa e aprova.
 */
@Getter
@AllArgsConstructor
public class Suggestion {
    private UUID id;
    private SuggestionType type;
    private String reason;

    // Dados do lançamento (proposto, no caso de ADD/FIX_DATE/MOVE_ACCOUNT; do que já existe, no caso de REMOVE)
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private TransactionType transactionType;

    // Preenchido quando a sugestão envolve uma transação já existente (REMOVE/FIX_DATE/MOVE_ACCOUNT)
    private UUID existingTransactionId;
    private LocalDate currentDate;
    private String currentAccountName;
}
