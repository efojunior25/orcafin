package com.orcafin.service;

import com.orcafin.dto.TransactionRequest;
import com.orcafin.entity.Account;
import com.orcafin.entity.Category;
import com.orcafin.entity.CategoryType;
import com.orcafin.entity.CreditCard;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.repository.AccountRepository;
import com.orcafin.repository.CategoryRepository;
import com.orcafin.repository.CreditCardRepository;
import com.orcafin.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CreditCardService creditCardService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;
    private Account otherAccount;
    private Category despesaCategory;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());

        account = new Account();
        account.setId(UUID.randomUUID());
        account.setUser(user);
        account.setBalance(new BigDecimal("100.00"));

        otherAccount = new Account();
        otherAccount.setId(UUID.randomUUID());
        otherAccount.setUser(user);
        otherAccount.setBalance(new BigDecimal("50.00"));

        despesaCategory = new Category();
        despesaCategory.setId(UUID.randomUUID());
        despesaCategory.setType(CategoryType.DESPESA);
        despesaCategory.setUser(null);

        lenient().when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private TransactionRequest baseRequest(TransactionType type, BigDecimal amount) {
        TransactionRequest request = new TransactionRequest();
        request.setType(type);
        request.setAmount(amount);
        request.setDate(LocalDate.now());
        request.setPaymentMethod(com.orcafin.entity.PaymentMethod.DEBITO);
        return request;
    }

    @Test
    void despesaSubtractsFromAccountBalance() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(categoryRepository.findById(despesaCategory.getId())).thenReturn(Optional.of(despesaCategory));

        TransactionRequest request = baseRequest(TransactionType.DESPESA, new BigDecimal("30.00"));
        request.setAccountId(account.getId());
        request.setCategoryId(despesaCategory.getId());

        transactionService.createTransaction(user, request);

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
    }

    @Test
    void receitaAddsToAccountBalance() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(categoryRepository.findById(despesaCategory.getId())).thenReturn(Optional.of(despesaCategory));

        TransactionRequest request = baseRequest(TransactionType.RECEITA, new BigDecimal("25.00"));
        request.setAccountId(account.getId());
        request.setCategoryId(despesaCategory.getId());

        transactionService.createTransaction(user, request);

        assertThat(account.getBalance()).isEqualByComparingTo("125.00");
    }

    @Test
    void transferMovesBalanceBetweenAccountsWithoutTouchingCategoryOrTotals() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.findById(otherAccount.getId())).thenReturn(Optional.of(otherAccount));

        TransactionRequest request = baseRequest(TransactionType.TRANSFERENCIA, new BigDecimal("40.00"));
        request.setAccountId(account.getId());
        request.setDestinationAccountId(otherAccount.getId());

        transactionService.createTransaction(user, request);

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
        assertThat(otherAccount.getBalance()).isEqualByComparingTo("90.00");
    }

    @Test
    void transferToSameAccountIsRejected() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        TransactionRequest request = baseRequest(TransactionType.TRANSFERENCIA, new BigDecimal("10.00"));
        request.setAccountId(account.getId());
        request.setDestinationAccountId(account.getId());

        assertThatThrownBy(() -> transactionService.createTransaction(user, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creditCardPurchaseDoesNotTouchAnyAccountBalance() {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(UUID.randomUUID());
        creditCard.setUser(user);
        creditCard.setCreditLimit(new BigDecimal("1000.00"));

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));
        when(categoryRepository.findById(despesaCategory.getId())).thenReturn(Optional.of(despesaCategory));
        when(creditCardService.usedLimit(creditCard)).thenReturn(BigDecimal.ZERO);

        TransactionRequest request = baseRequest(TransactionType.DESPESA, new BigDecimal("99.90"));
        request.setCreditCardId(creditCard.getId());
        request.setCategoryId(despesaCategory.getId());

        transactionService.createTransaction(user, request);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        assertThat(otherAccount.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void purchaseExceedingAvailableLimitIsRejected() {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(UUID.randomUUID());
        creditCard.setUser(user);
        creditCard.setCreditLimit(new BigDecimal("100.00"));

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));
        when(categoryRepository.findById(despesaCategory.getId())).thenReturn(Optional.of(despesaCategory));
        when(creditCardService.usedLimit(creditCard)).thenReturn(new BigDecimal("80.00"));

        TransactionRequest request = baseRequest(TransactionType.DESPESA, new BigDecimal("50.00"));
        request.setCreditCardId(creditCard.getId());
        request.setCategoryId(despesaCategory.getId());

        assertThatThrownBy(() -> transactionService.createTransaction(user, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Limite disponível");
    }

    @Test
    void installmentPurchaseGeneratesOneTransactionPerMonthSummingToTotalAmount() {
        CreditCard creditCard = new CreditCard();
        creditCard.setId(UUID.randomUUID());
        creditCard.setUser(user);
        creditCard.setCreditLimit(new BigDecimal("1000.00"));

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));
        when(categoryRepository.findById(despesaCategory.getId())).thenReturn(Optional.of(despesaCategory));
        when(creditCardService.usedLimit(creditCard)).thenReturn(BigDecimal.ZERO);

        TransactionRequest request = baseRequest(TransactionType.DESPESA, new BigDecimal("100.00"));
        request.setCreditCardId(creditCard.getId());
        request.setCategoryId(despesaCategory.getId());
        request.setDescription("Notebook");
        request.setInstallments(3);

        transactionService.createTransaction(user, request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(3)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();

        BigDecimal sum = saved.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
        assertThat(saved.get(0).getDate()).isEqualTo(request.getDate());
        assertThat(saved.get(1).getDate()).isEqualTo(request.getDate().plusMonths(1));
        assertThat(saved.get(2).getDate()).isEqualTo(request.getDate().plusMonths(2));
        assertThat(saved.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(saved.get(2).getInstallmentNumber()).isEqualTo(3);
        assertThat(saved).allMatch(t -> t.getInstallmentTotal() == 3);
        assertThat(saved).allMatch(t -> t.getInstallmentGroupId() != null);
        assertThat(saved.get(0).getDescription()).isEqualTo("Notebook (1/3)");
    }

    @Test
    void deletingDespesaRevertsAccountBalance() {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setType(TransactionType.DESPESA);
        transaction.setAmount(new BigDecimal("20.00"));
        account.setBalance(new BigDecimal("80.00"));

        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(user, transaction.getId());

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void accessingAnotherUsersAccountIsForbidden() {
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        account.setUser(otherUser);

        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        TransactionRequest request = baseRequest(TransactionType.DESPESA, new BigDecimal("10.00"));
        request.setAccountId(account.getId());
        request.setCategoryId(despesaCategory.getId());

        assertThatThrownBy(() -> transactionService.createTransaction(user, request))
                .isInstanceOf(ForbiddenException.class);
    }
}
