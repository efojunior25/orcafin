package com.orcafin.service;

import com.orcafin.dto.StatementEntry;
import com.orcafin.dto.Suggestion;
import com.orcafin.entity.Account;
import com.orcafin.entity.SuggestionType;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    private User user;
    private Account contaCerta;
    private Account contaErrada;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        contaCerta = new Account();
        contaCerta.setId(UUID.randomUUID());
        contaCerta.setUser(user);
        contaCerta.setName("Banco do Brasil");

        contaErrada = new Account();
        contaErrada.setId(UUID.randomUUID());
        contaErrada.setUser(user);
        contaErrada.setName("Nubank");
    }

    private Transaction tx(Account account, TransactionType type, String amount, LocalDate date, String desc) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setUser(user);
        t.setAccount(account);
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setDate(date);
        t.setDescription(desc);
        return t;
    }

    @Test
    void matchedEntryGeneratesNoSuggestion() {
        Transaction existing = tx(contaCerta, TransactionType.DESPESA, "129.95", LocalDate.of(2026, 8, 4), "Unifatecie");
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any())).thenReturn(List.of(existing));

        List<StatementEntry> entries = List.of(
                new StatementEntry(LocalDate.of(2026, 8, 4), "Unifatecie", new BigDecimal("129.95"), TransactionType.DESPESA));

        List<Suggestion> result = reconciliationService.reconcile(user, entries, contaCerta.getId(), null);

        assertThat(result).isEmpty();
    }

    @Test
    void entryMissingFromAppGeneratesAddSuggestion() {
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any())).thenReturn(List.of());

        List<StatementEntry> entries = List.of(
                new StatementEntry(LocalDate.of(2026, 8, 7), "Pix Sergio", new BigDecimal("11.00"), TransactionType.DESPESA));

        List<Suggestion> result = reconciliationService.reconcile(user, entries, contaCerta.getId(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(SuggestionType.ADD);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("11.00");
    }

    @Test
    void extraAppTransactionNotInStatementGeneratesRemoveSuggestion() {
        Transaction duplicata = tx(contaCerta, TransactionType.DESPESA, "150.00", LocalDate.of(2026, 8, 8), "Aparelho Blu Ray");
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any())).thenReturn(List.of(duplicata));

        List<StatementEntry> entries = List.of(
                new StatementEntry(LocalDate.of(2026, 8, 8), "Outra coisa", new BigDecimal("99.90"), TransactionType.DESPESA));

        List<Suggestion> result = reconciliationService.reconcile(user, entries, contaCerta.getId(), null);

        assertThat(result).anySatisfy(s -> {
            assertThat(s.getType()).isEqualTo(SuggestionType.REMOVE);
            assertThat(s.getExistingTransactionId()).isEqualTo(duplicata.getId());
        });
    }

    @Test
    void transactionOnWrongAccountGeneratesMoveAccountSuggestion() {
        Transaction naContaErrada = tx(contaErrada, TransactionType.DESPESA, "352.00", LocalDate.of(2026, 8, 8), "Cartão Meriane");
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any())).thenReturn(List.of(naContaErrada));

        List<StatementEntry> entries = List.of(
                new StatementEntry(LocalDate.of(2026, 8, 8), "Cartão Meriane", new BigDecimal("352.00"), TransactionType.DESPESA));

        List<Suggestion> result = reconciliationService.reconcile(user, entries, contaCerta.getId(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(SuggestionType.MOVE_ACCOUNT);
        assertThat(result.get(0).getExistingTransactionId()).isEqualTo(naContaErrada.getId());
        assertThat(result.get(0).getCurrentAccountName()).isEqualTo("Nubank");
    }

    @Test
    void transactionWithWrongDateGeneratesFixDateSuggestion() {
        Transaction dataErrada = tx(contaCerta, TransactionType.DESPESA, "82.57", LocalDate.of(2026, 8, 8), "Neoenergia COSERN");
        when(transactionRepository.findByUserIdAndDateBetween(any(), any(), any())).thenReturn(List.of(dataErrada));

        List<StatementEntry> entries = List.of(
                new StatementEntry(LocalDate.of(2026, 8, 5), "Neoenergia COSERN", new BigDecimal("82.57"), TransactionType.DESPESA));

        List<Suggestion> result = reconciliationService.reconcile(user, entries, contaCerta.getId(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(SuggestionType.FIX_DATE);
        assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(result.get(0).getCurrentDate()).isEqualTo(LocalDate.of(2026, 8, 8));
    }
}
