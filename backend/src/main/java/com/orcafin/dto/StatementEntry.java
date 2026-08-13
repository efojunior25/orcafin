package com.orcafin.dto;

import com.orcafin.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um lançamento extraído de um extrato/fatura enviado pelo usuário (ainda não é uma Transaction). */
@Getter
@AllArgsConstructor
public class StatementEntry {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    /** RECEITA (entrada/crédito/estorno) ou DESPESA (saída/compra). */
    private TransactionType type;
}
