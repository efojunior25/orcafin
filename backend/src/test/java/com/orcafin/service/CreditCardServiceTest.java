package com.orcafin.service;

import com.orcafin.entity.CreditCard;
import com.orcafin.entity.PaymentMethod;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.repository.CreditCardRepository;
import com.orcafin.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private CreditCardService creditCardService;

    private User user;
    private CreditCard creditCard;

    @BeforeEach
    void setUp() {
        creditCardService = new CreditCardService(creditCardRepository, transactionRepository);
        user = new User();
        user.setId(UUID.randomUUID());
        creditCard = new CreditCard();
        creditCard.setId(UUID.randomUUID());
        creditCard.setUser(user);
        creditCard.setClosingDay(5);
        creditCard.setDueDay(12);
        creditCard.setCreditLimit(new BigDecimal("1000"));
    }

    private Transaction fakeTransaction(TransactionType type, String amount) {
        Transaction t = new Transaction();
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setPaymentMethod(PaymentMethod.CREDITO);
        return t;
    }

    @Test
    void invoiceTotalSumsDespesaAndSubtractsReceita() {
        when(creditCardRepository.findByUserId(user.getId())).thenReturn(List.of(creditCard));
        when(transactionRepository.findByCreditCardIdAndDateBetween(eq(creditCard.getId()), any(), any()))
                .thenReturn(List.of(
                        fakeTransaction(TransactionType.DESPESA, "100.00"),
                        fakeTransaction(TransactionType.DESPESA, "50.00"),
                        fakeTransaction(TransactionType.RECEITA, "30.00")
                ));

        var result = creditCardService.listCreditCards(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentInvoiceTotal()).isEqualByComparingTo("120.00");
    }

    @Test
    void invoicePeriodEndsOnClosingDayAndCoversOneMonth() {
        when(creditCardRepository.findByUserId(user.getId())).thenReturn(List.of(creditCard));
        when(transactionRepository.findByCreditCardIdAndDateBetween(eq(creditCard.getId()), any(), any()))
                .thenReturn(List.of());

        creditCardService.listCreditCards(user);

        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(transactionRepository)
                .findByCreditCardIdAndDateBetween(eq(creditCard.getId()), startCaptor.capture(), endCaptor.capture());

        LocalDate start = startCaptor.getValue();
        LocalDate end = endCaptor.getValue();
        LocalDate today = LocalDate.now();

        assertThat(end.getDayOfMonth()).isEqualTo(Math.min(creditCard.getClosingDay(), end.lengthOfMonth()));
        assertThat(end).isAfterOrEqualTo(today.minusMonths(1));
        assertThat(start).isEqualTo(YearMonth.from(end).minusMonths(1)
                .atDay(Math.min(creditCard.getClosingDay(), YearMonth.from(end).minusMonths(1).lengthOfMonth()))
                .plusDays(1));
        // A compra feita exatamente no dia de fechamento deve cair na fatura atual (end == closing date).
        assertThat(today.isAfter(end) || !today.isBefore(start)).isTrue();
    }
}
