package com.orcafin.controller;

import com.orcafin.dto.ParseTransactionRequest;
import com.orcafin.dto.ParseTransactionResponse;
import com.orcafin.dto.TransactionRequest;
import com.orcafin.dto.TransactionResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.AiParsingService;
import com.orcafin.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AiParsingService aiParsingService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(transactionService.listTransactions(user, from, to));
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(transactionService.createTransaction(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(transactionService.updateTransaction(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        transactionService.deleteTransaction(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/parse")
    public ResponseEntity<ParseTransactionResponse> parse(@Valid @RequestBody ParseTransactionRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(aiParsingService.parse(user, request.getText()));
    }
}
