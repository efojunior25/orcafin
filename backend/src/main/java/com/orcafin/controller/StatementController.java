package com.orcafin.controller;

import com.orcafin.dto.ApplySuggestionsRequest;
import com.orcafin.dto.ApplySuggestionsResponse;
import com.orcafin.dto.ImportStatementResponse;
import com.orcafin.dto.StatementEntry;
import com.orcafin.dto.Suggestion;
import com.orcafin.dto.TransactionRequest;
import com.orcafin.entity.Category;
import com.orcafin.entity.CategoryType;
import com.orcafin.entity.SuggestionType;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.CategoryRepository;
import com.orcafin.repository.TransactionRepository;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.AiParsingService;
import com.orcafin.service.ReconciliationService;
import com.orcafin.service.ReconciliationSessionService;
import com.orcafin.service.StatementParsingService;
import com.orcafin.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementParsingService statementParsingService;
    private final AiParsingService aiParsingService;
    private final ReconciliationService reconciliationService;
    private final ReconciliationSessionService sessionService;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<ImportStatementResponse> importStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID creditCardId) {
        User user = SecurityUtils.getCurrentUser();
        if ((accountId == null) == (creditCardId == null)) {
            throw new IllegalArgumentException("Informe exatamente uma conta ou um cartão de destino.");
        }

        StatementParsingService.Format format = statementParsingService.detectFormat(file.getOriginalFilename());
        String rawText = statementParsingService.extractRawText(file, format);

        List<StatementEntry> entries = format == StatementParsingService.Format.OFX
                ? statementParsingService.parseOfx(rawText)
                : aiParsingService.extractStatementEntries(rawText);

        List<Suggestion> suggestions = reconciliationService.reconcile(user, entries, accountId, creditCardId);
        UUID sessionId = sessionService.create(user.getId(), accountId, creditCardId, suggestions);

        return ResponseEntity.ok(new ImportStatementResponse(sessionId, suggestions));
    }

    @PostMapping("/{sessionId}/apply")
    public ResponseEntity<ApplySuggestionsResponse> apply(@PathVariable UUID sessionId,
                                                            @Valid @RequestBody ApplySuggestionsRequest request) {
        User user = SecurityUtils.getCurrentUser();
        ReconciliationSessionService.Session session = sessionService.get(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão de importação expirada ou não encontrada."));
        if (!session.userId().equals(user.getId())) {
            throw new ResourceNotFoundException("Sessão de importação expirada ou não encontrada.");
        }

        int applied = 0;
        for (Suggestion suggestion : session.suggestions()) {
            if (!request.getApprovedSuggestionIds().contains(suggestion.getId())) {
                continue;
            }
            applySuggestion(user, suggestion, session.targetAccountId(), session.targetCreditCardId());
            applied++;
        }
        sessionService.remove(sessionId);

        return ResponseEntity.ok(new ApplySuggestionsResponse(applied, applied + " correção(ões) aplicada(s)."));
    }

    private void applySuggestion(User user, Suggestion suggestion, UUID targetAccountId, UUID targetCreditCardId) {
        switch (suggestion.getType()) {
            case ADD -> {
                TransactionRequest request = new TransactionRequest();
                request.setAccountId(targetAccountId);
                request.setCreditCardId(targetCreditCardId);
                request.setCategoryId(defaultCategoryId(user, suggestion.getTransactionType()));
                request.setType(suggestion.getTransactionType());
                request.setAmount(suggestion.getAmount());
                request.setDescription(suggestion.getDescription());
                request.setDate(suggestion.getDate());
                request.setPaymentMethod(targetCreditCardId != null
                        ? com.orcafin.entity.PaymentMethod.CREDITO
                        : com.orcafin.entity.PaymentMethod.DINHEIRO);
                transactionService.createTransaction(user, request);
            }
            case REMOVE -> transactionService.deleteTransaction(user, suggestion.getExistingTransactionId());
            case FIX_DATE -> {
                TransactionRequest request = requestFromExisting(suggestion.getExistingTransactionId());
                request.setDate(suggestion.getDate());
                transactionService.updateTransaction(user, suggestion.getExistingTransactionId(), request);
            }
            case MOVE_ACCOUNT -> {
                TransactionRequest request = requestFromExisting(suggestion.getExistingTransactionId());
                request.setAccountId(targetAccountId);
                request.setCreditCardId(targetCreditCardId);
                transactionService.updateTransaction(user, suggestion.getExistingTransactionId(), request);
            }
        }
    }

    private TransactionRequest requestFromExisting(UUID transactionId) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado"));
        TransactionRequest request = new TransactionRequest();
        request.setAccountId(t.getAccount() != null ? t.getAccount().getId() : null);
        request.setCreditCardId(t.getCreditCard() != null ? t.getCreditCard().getId() : null);
        request.setCategoryId(t.getCategory() != null ? t.getCategory().getId() : null);
        request.setDestinationAccountId(t.getDestinationAccount() != null ? t.getDestinationAccount().getId() : null);
        request.setType(t.getType());
        request.setGroup(t.getGroup());
        request.setAmount(t.getAmount());
        request.setDescription(t.getDescription());
        request.setDate(t.getDate());
        request.setPaymentMethod(t.getPaymentMethod());
        request.setRecurring(t.isRecurring());
        request.setRecurrenceFrequency(t.getRecurrenceFrequency());
        request.setRecurrenceEndDate(t.getRecurrenceEndDate());
        return request;
    }

    private UUID defaultCategoryId(User user, TransactionType type) {
        CategoryType categoryType = type == TransactionType.RECEITA ? CategoryType.RECEITA : CategoryType.DESPESA;
        List<Category> categories = categoryRepository.findByUserIdOrUserIdIsNull(user.getId());
        Optional<Category> outros = categories.stream()
                .filter(c -> c.getType() == categoryType && "Outros".equalsIgnoreCase(c.getName()))
                .findFirst();
        return outros.map(Category::getId)
                .orElseThrow(() -> new IllegalArgumentException("Nenhuma categoria \"Outros\" encontrada pra classificar o lançamento."));
    }
}
