package com.orcafin.service;

import com.orcafin.dto.TransactionRequest;
import com.orcafin.dto.TransactionResponse;
import com.orcafin.entity.Account;
import com.orcafin.entity.Category;
import com.orcafin.entity.CreditCard;
import com.orcafin.entity.PrepaidCard;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.AccountRepository;
import com.orcafin.repository.CategoryRepository;
import com.orcafin.repository.CreditCardRepository;
import com.orcafin.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditCardService creditCardService;
    private final PrepaidCardService prepaidCardService;

    // Sem filtro de data, o app pode acumular anos de histórico; limita pra evitar
    // consultas/payloads gigantes num único request (o histórico é sempre navegado
    // por mês na UI, então isso só afeta chamadas diretas à API sem from/to).
    private static final int MAX_UNFILTERED_RESULTS = 1000;

    public List<TransactionResponse> listTransactions(User user, LocalDate from, LocalDate to) {
        List<Transaction> transactions;
        if (from != null && to != null) {
            transactions = transactionRepository.findByUserIdAndDateBetween(user.getId(), from, to);
        } else {
            transactions = transactionRepository.findByUserIdOrderByDateDesc(
                    user.getId(), PageRequest.of(0, MAX_UNFILTERED_RESULTS));
        }
        return transactions.stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse createTransaction(User user, TransactionRequest request) {
        boolean isTransfer = request.getType() == TransactionType.TRANSFERENCIA;

        Account account = isTransfer
                ? getOwnedAccount(user, request.getAccountId())
                : resolveAccountForNonTransfer(user, request);
        CreditCard creditCard = (!isTransfer && request.getAccountId() == null && request.getCreditCardId() != null)
                ? getOwnedCreditCard(user, request.getCreditCardId())
                : null;
        PrepaidCard prepaidCard = (!isTransfer && request.getAccountId() == null && creditCard == null)
                ? getOwnedPrepaidCard(user, request.getPrepaidCardId())
                : null;
        Category category = isTransfer ? null : getAccessibleCategory(user, request.getCategoryId());
        Account destinationAccount = isTransfer ? getOwnedAccount(user, requireDestinationAccountId(request)) : null;
        if (isTransfer && destinationAccount.getId().equals(account.getId())) {
            throw new IllegalArgumentException("A conta de destino deve ser diferente da conta de origem");
        }

        checkCreditLimit(creditCard, request.getType(), request.getAmount(), null);

        boolean isInstallment = creditCard != null
                && request.getType() == TransactionType.DESPESA
                && request.getInstallments() != null
                && request.getInstallments() > 1;

        if (isInstallment) {
            return createInstallments(user, request, creditCard, category);
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCreditCard(creditCard);
        transaction.setPrepaidCard(prepaidCard);
        transaction.setCategory(category);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setType(request.getType());
        transaction.setGroup(request.getGroup());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setPayee(request.getPayee());
        transaction.setDate(request.getDate());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setRecurring(request.isRecurring());
        transaction.setRecurrenceFrequency(request.getRecurrenceFrequency());
        transaction.setRecurrenceEndDate(request.getRecurrenceEndDate());

        if (account != null) {
            applyBalance(account, destinationAccount, transaction.getType(), transaction.getAmount());
            accountRepository.save(account);
            if (destinationAccount != null) {
                accountRepository.save(destinationAccount);
            }
        }
        if (prepaidCard != null) {
            applyPrepaidBalance(prepaidCard, transaction.getType(), transaction.getAmount());
        }
        transactionRepository.save(transaction);

        return new TransactionResponse(transaction);
    }

    /**
     * Divide o valor total em N parcelas mensais lançadas na fatura de cada mês.
     * A última parcela absorve o resto do arredondamento pra soma bater o valor exato.
     */
    private TransactionResponse createInstallments(User user, TransactionRequest request, CreditCard creditCard, Category category) {
        int count = request.getInstallments();
        BigDecimal total = request.getAmount();
        BigDecimal base = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal lastAmount = total.subtract(base.multiply(BigDecimal.valueOf(count - 1)));

        UUID groupId = UUID.randomUUID();
        Transaction first = null;
        for (int i = 0; i < count; i++) {
            Transaction installment = new Transaction();
            installment.setUser(user);
            installment.setCreditCard(creditCard);
            installment.setCategory(category);
            installment.setType(TransactionType.DESPESA);
            installment.setGroup(request.getGroup());
            installment.setAmount(i == count - 1 ? lastAmount : base);
            installment.setDescription(request.getDescription() + " (" + (i + 1) + "/" + count + ")");
            installment.setPayee(request.getPayee());
            installment.setDate(request.getDate().plusMonths(i));
            installment.setPaymentMethod(request.getPaymentMethod());
            installment.setInstallmentGroupId(groupId);
            installment.setInstallmentNumber(i + 1);
            installment.setInstallmentTotal(count);
            transactionRepository.save(installment);
            if (i == 0) {
                first = installment;
            }
        }
        return new TransactionResponse(first);
    }

    @Transactional
    public TransactionResponse updateTransaction(User user, UUID transactionId, TransactionRequest request) {
        Transaction transaction = getOwnedTransaction(user, transactionId);

        Account oldAccount = transaction.getAccount();
        Account oldDestinationAccount = transaction.getDestinationAccount();
        if (oldAccount != null) {
            revertBalance(oldAccount, oldDestinationAccount, transaction.getType(), transaction.getAmount());
        }
        PrepaidCard oldPrepaidCard = transaction.getPrepaidCard();
        if (oldPrepaidCard != null) {
            revertPrepaidBalance(oldPrepaidCard, transaction.getType(), transaction.getAmount());
        }

        boolean isTransfer = request.getType() == TransactionType.TRANSFERENCIA;
        Account newAccount = isTransfer
                ? getOwnedAccount(user, request.getAccountId())
                : resolveAccountForNonTransfer(user, request);
        CreditCard newCreditCard = (!isTransfer && request.getAccountId() == null && request.getCreditCardId() != null)
                ? getOwnedCreditCard(user, request.getCreditCardId())
                : null;
        PrepaidCard newPrepaidCard = (!isTransfer && request.getAccountId() == null && newCreditCard == null)
                ? getOwnedPrepaidCard(user, request.getPrepaidCardId())
                : null;
        Category newCategory = isTransfer ? null : getAccessibleCategory(user, request.getCategoryId());
        Account newDestinationAccount = isTransfer ? getOwnedAccount(user, requireDestinationAccountId(request)) : null;
        if (isTransfer && newDestinationAccount.getId().equals(newAccount.getId())) {
            throw new IllegalArgumentException("A conta de destino deve ser diferente da conta de origem");
        }

        checkCreditLimit(newCreditCard, request.getType(), request.getAmount(), transaction);

        transaction.setAccount(newAccount);
        transaction.setCreditCard(newCreditCard);
        transaction.setPrepaidCard(newPrepaidCard);
        transaction.setCategory(newCategory);
        transaction.setDestinationAccount(newDestinationAccount);
        transaction.setType(request.getType());
        transaction.setGroup(request.getGroup());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setPayee(request.getPayee());
        transaction.setDate(request.getDate());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setRecurring(request.isRecurring());
        transaction.setRecurrenceFrequency(request.getRecurrenceFrequency());
        transaction.setRecurrenceEndDate(request.getRecurrenceEndDate());

        if (oldAccount != null) {
            accountRepository.save(oldAccount);
        }
        if (oldDestinationAccount != null) {
            accountRepository.save(oldDestinationAccount);
        }
        if (newAccount != null) {
            applyBalance(newAccount, newDestinationAccount, transaction.getType(), transaction.getAmount());
            accountRepository.save(newAccount);
        }
        if (newDestinationAccount != null) {
            accountRepository.save(newDestinationAccount);
        }
        if (newPrepaidCard != null) {
            applyPrepaidBalance(newPrepaidCard, transaction.getType(), transaction.getAmount());
        }
        transactionRepository.save(transaction);

        return new TransactionResponse(transaction);
    }

    @Transactional
    public void deleteTransaction(User user, UUID transactionId) {
        Transaction transaction = getOwnedTransaction(user, transactionId);
        Account account = transaction.getAccount();
        Account destinationAccount = transaction.getDestinationAccount();
        if (account != null) {
            revertBalance(account, destinationAccount, transaction.getType(), transaction.getAmount());
            accountRepository.save(account);
            if (destinationAccount != null) {
                accountRepository.save(destinationAccount);
            }
        }
        PrepaidCard prepaidCard = transaction.getPrepaidCard();
        if (prepaidCard != null) {
            revertPrepaidBalance(prepaidCard, transaction.getType(), transaction.getAmount());
        }
        transactionRepository.delete(transaction);
    }

    /**
     * Bloqueia despesas que ultrapassariam o limite disponível do cartão. `excluding`
     * é a transação sendo editada (se houver) — sua contribuição atual pro limite usado
     * é descontada antes de simular o novo valor, senão ela contaria em dobro.
     */
    private void checkCreditLimit(CreditCard creditCard, TransactionType type, BigDecimal amount, Transaction excluding) {
        if (creditCard == null || type != TransactionType.DESPESA) {
            return;
        }
        BigDecimal used = creditCardService.usedLimit(creditCard);
        if (excluding != null && excluding.getCreditCard() != null
                && excluding.getCreditCard().getId().equals(creditCard.getId())) {
            if (excluding.getType() == TransactionType.DESPESA) {
                used = used.subtract(excluding.getAmount());
            } else if (excluding.getType() == TransactionType.RECEITA) {
                used = used.add(excluding.getAmount());
            }
        }
        BigDecimal projected = used.add(amount);
        if (projected.compareTo(creditCard.getCreditLimit()) > 0) {
            BigDecimal available = creditCard.getCreditLimit().subtract(used).max(BigDecimal.ZERO);
            throw new IllegalArgumentException(
                    "Limite disponível no cartão: R$ " + available.setScale(2, RoundingMode.HALF_UP)
                            + ". Essa compra ultrapassa o limite.");
        }
    }

    private Account resolveAccountForNonTransfer(User user, TransactionRequest request) {
        if (request.getAccountId() == null) {
            return null;
        }
        return getOwnedAccount(user, request.getAccountId());
    }

    private CreditCard getOwnedCreditCard(User user, UUID creditCardId) {
        if (creditCardId == null) {
            throw new IllegalArgumentException("Informe uma conta ou um cartão de crédito");
        }
        CreditCard creditCard = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão de crédito não encontrado"));
        if (!creditCard.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a este cartão");
        }
        return creditCard;
    }

    private PrepaidCard getOwnedPrepaidCard(User user, UUID prepaidCardId) {
        if (prepaidCardId == null) {
            throw new IllegalArgumentException("Informe uma conta, um cartão de crédito ou um cartão pré-pago");
        }
        return prepaidCardService.getOwnedPrepaidCard(user, prepaidCardId);
    }

    private void applyPrepaidBalance(PrepaidCard prepaidCard, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.RECEITA) {
            prepaidCardService.credit(prepaidCard, amount);
        } else if (type == TransactionType.DESPESA) {
            prepaidCardService.debit(prepaidCard, amount);
        }
    }

    private void revertPrepaidBalance(PrepaidCard prepaidCard, TransactionType type, BigDecimal amount) {
        if (type == TransactionType.RECEITA) {
            prepaidCardService.debit(prepaidCard, amount);
        } else if (type == TransactionType.DESPESA) {
            prepaidCardService.credit(prepaidCard, amount);
        }
    }

    private UUID requireDestinationAccountId(TransactionRequest request) {
        if (request.getDestinationAccountId() == null) {
            throw new IllegalArgumentException("Conta de destino é obrigatória para transferências");
        }
        return request.getDestinationAccountId();
    }

    private void applyBalance(Account account, Account destinationAccount, TransactionType type, BigDecimal amount) {
        switch (type) {
            case RECEITA -> account.setBalance(account.getBalance().add(amount));
            case DESPESA -> account.setBalance(account.getBalance().subtract(amount));
            case TRANSFERENCIA -> {
                account.setBalance(account.getBalance().subtract(amount));
                destinationAccount.setBalance(destinationAccount.getBalance().add(amount));
            }
        }
    }

    private void revertBalance(Account account, Account destinationAccount, TransactionType type, BigDecimal amount) {
        switch (type) {
            case RECEITA -> account.setBalance(account.getBalance().subtract(amount));
            case DESPESA -> account.setBalance(account.getBalance().add(amount));
            case TRANSFERENCIA -> {
                account.setBalance(account.getBalance().add(amount));
                if (destinationAccount != null) {
                    destinationAccount.setBalance(destinationAccount.getBalance().subtract(amount));
                }
            }
        }
    }

    private Account getOwnedAccount(User user, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta conta");
        }
        return account;
    }

    private Category getAccessibleCategory(User user, UUID categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Categoria é obrigatória");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta categoria");
        }
        return category;
    }

    private Transaction getOwnedTransaction(User user, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Lançamento não encontrado"));
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a este lançamento");
        }
        return transaction;
    }
}
