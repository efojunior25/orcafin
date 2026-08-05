package com.orcafin.service;

import com.orcafin.dto.TransactionRequest;
import com.orcafin.dto.TransactionResponse;
import com.orcafin.entity.Account;
import com.orcafin.entity.Category;
import com.orcafin.entity.CreditCard;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public List<TransactionResponse> listTransactions(User user, LocalDate from, LocalDate to) {
        List<Transaction> transactions;
        if (from != null && to != null) {
            transactions = transactionRepository.findByUserIdAndDateBetween(user.getId(), from, to);
        } else {
            transactions = transactionRepository.findByUserIdOrderByDateDesc(user.getId());
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
        CreditCard creditCard = (!isTransfer && request.getAccountId() == null)
                ? getOwnedCreditCard(user, request.getCreditCardId())
                : null;
        Category category = getAccessibleCategory(user, request.getCategoryId());
        Account destinationAccount = isTransfer ? getOwnedAccount(user, requireDestinationAccountId(request)) : null;
        if (isTransfer && destinationAccount.getId().equals(account.getId())) {
            throw new IllegalArgumentException("A conta de destino deve ser diferente da conta de origem");
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setCreditCard(creditCard);
        transaction.setCategory(category);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setType(request.getType());
        transaction.setGroup(request.getGroup());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
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
        transactionRepository.save(transaction);

        return new TransactionResponse(transaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(User user, UUID transactionId, TransactionRequest request) {
        Transaction transaction = getOwnedTransaction(user, transactionId);

        Account oldAccount = transaction.getAccount();
        Account oldDestinationAccount = transaction.getDestinationAccount();
        if (oldAccount != null) {
            revertBalance(oldAccount, oldDestinationAccount, transaction.getType(), transaction.getAmount());
        }

        boolean isTransfer = request.getType() == TransactionType.TRANSFERENCIA;
        Account newAccount = isTransfer
                ? getOwnedAccount(user, request.getAccountId())
                : resolveAccountForNonTransfer(user, request);
        CreditCard newCreditCard = (!isTransfer && request.getAccountId() == null)
                ? getOwnedCreditCard(user, request.getCreditCardId())
                : null;
        Category newCategory = getAccessibleCategory(user, request.getCategoryId());
        Account newDestinationAccount = isTransfer ? getOwnedAccount(user, requireDestinationAccountId(request)) : null;
        if (isTransfer && newDestinationAccount.getId().equals(newAccount.getId())) {
            throw new IllegalArgumentException("A conta de destino deve ser diferente da conta de origem");
        }

        transaction.setAccount(newAccount);
        transaction.setCreditCard(newCreditCard);
        transaction.setCategory(newCategory);
        transaction.setDestinationAccount(newDestinationAccount);
        transaction.setType(request.getType());
        transaction.setGroup(request.getGroup());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
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
        transactionRepository.delete(transaction);
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
