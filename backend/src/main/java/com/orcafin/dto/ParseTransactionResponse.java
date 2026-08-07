package com.orcafin.dto;

import com.orcafin.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ParseTransactionResponse {
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private LocalDate date;
    private String transcribedText;
}
