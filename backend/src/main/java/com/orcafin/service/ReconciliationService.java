package com.orcafin.service;

import com.orcafin.dto.StatementEntry;
import com.orcafin.dto.Suggestion;
import com.orcafin.entity.SuggestionType;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.User;
import com.orcafin.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Compara os lançamentos extraídos de um extrato com o que já está lançado no
 * app e gera sugestões de correção — mesmo processo feito manualmente ao
 * reconciliar extratos reais: achar lançamento faltando, duplicado, na conta
 * errada ou com a data errada. Nunca aplica nada sozinho.
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final int DATE_TOLERANCE_DAYS = 5;

    private final TransactionRepository transactionRepository;

    public List<Suggestion> reconcile(User user, List<StatementEntry> entries, UUID targetAccountId, UUID targetCreditCardId) {
        LocalDate minDate = entries.stream().map(StatementEntry::getDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate maxDate = entries.stream().map(StatementEntry::getDate).max(LocalDate::compareTo).orElseThrow();

        List<Transaction> candidates = new ArrayList<>(transactionRepository.findByUserIdAndDateBetween(
                user.getId(), minDate.minusDays(DATE_TOLERANCE_DAYS), maxDate.plusDays(DATE_TOLERANCE_DAYS)));

        List<Suggestion> suggestions = new ArrayList<>();

        for (StatementEntry entry : entries) {
            Transaction match = findBestMatch(candidates, entry, targetAccountId, targetCreditCardId);
            if (match == null) {
                suggestions.add(new Suggestion(UUID.randomUUID(), SuggestionType.ADD,
                        "Não encontrado no app — provável lançamento faltando.",
                        entry.getDate(), entry.getDescription(), entry.getAmount(), entry.getType(),
                        null, null, null));
                continue;
            }
            candidates.remove(match);

            boolean onTarget = isOnTarget(match, targetAccountId, targetCreditCardId);
            boolean sameDate = match.getDate().equals(entry.getDate());

            if (onTarget && sameDate) {
                continue; // já está certo, nenhuma sugestão
            }
            if (onTarget) {
                suggestions.add(new Suggestion(UUID.randomUUID(), SuggestionType.FIX_DATE,
                        "Valor bate, mas a data no app (" + match.getDate() + ") é diferente do extrato.",
                        entry.getDate(), match.getDescription(), match.getAmount(), match.getType(),
                        match.getId(), match.getDate(), accountOrCardName(match)));
            } else {
                suggestions.add(new Suggestion(UUID.randomUUID(), SuggestionType.MOVE_ACCOUNT,
                        "Esse lançamento está em \"" + accountOrCardName(match) + "\" no app, mas o extrato mostra que é daqui.",
                        entry.getDate(), match.getDescription(), match.getAmount(), match.getType(),
                        match.getId(), match.getDate(), accountOrCardName(match)));
            }
        }

        // Sobras: transações do app na conta/cartão alvo, dentro do período do extrato,
        // que nenhum lançamento do extrato reivindicou — prováveis duplicatas/erradas.
        for (Transaction leftover : candidates) {
            if (!isOnTarget(leftover, targetAccountId, targetCreditCardId)) continue;
            if (leftover.getDate().isBefore(minDate) || leftover.getDate().isAfter(maxDate)) continue;
            suggestions.add(new Suggestion(UUID.randomUUID(), SuggestionType.REMOVE,
                    "Está lançado no app mas não aparece no extrato enviado — pode ser duplicata.",
                    leftover.getDate(), leftover.getDescription(), leftover.getAmount(), leftover.getType(),
                    leftover.getId(), leftover.getDate(), accountOrCardName(leftover)));
        }

        suggestions.sort(Comparator.comparing(Suggestion::getDate));
        return suggestions;
    }

    private Transaction findBestMatch(List<Transaction> candidates, StatementEntry entry, UUID targetAccountId, UUID targetCreditCardId) {
        Transaction best = null;
        for (Transaction t : candidates) {
            if (t.getType() != entry.getType()) continue;
            if (t.getAmount().compareTo(entry.getAmount()) != 0) continue;
            long diffDays = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(t.getDate(), entry.getDate()));
            if (diffDays > DATE_TOLERANCE_DAYS) continue;

            boolean onTarget = isOnTarget(t, targetAccountId, targetCreditCardId);
            if (best == null) {
                best = t;
            } else {
                boolean bestOnTarget = isOnTarget(best, targetAccountId, targetCreditCardId);
                // Prioriza: na conta certa > mais perto da data do extrato.
                if (onTarget && !bestOnTarget) {
                    best = t;
                } else if (onTarget == bestOnTarget) {
                    long bestDiff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(best.getDate(), entry.getDate()));
                    if (diffDays < bestDiff) {
                        best = t;
                    }
                }
            }
        }
        return best;
    }

    private boolean isOnTarget(Transaction t, UUID targetAccountId, UUID targetCreditCardId) {
        if (targetAccountId != null) {
            return t.getAccount() != null && t.getAccount().getId().equals(targetAccountId);
        }
        if (targetCreditCardId != null) {
            return t.getCreditCard() != null && t.getCreditCard().getId().equals(targetCreditCardId);
        }
        return false;
    }

    private String accountOrCardName(Transaction t) {
        if (t.getAccount() != null) return t.getAccount().getName();
        if (t.getCreditCard() != null) return t.getCreditCard().getName();
        return "—";
    }
}
