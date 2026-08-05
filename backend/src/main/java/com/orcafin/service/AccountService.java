package com.orcafin.service;

import com.orcafin.dto.AccountRequest;
import com.orcafin.dto.AccountResponse;
import com.orcafin.entity.Account;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<AccountResponse> listAccounts(User user) {
        return accountRepository.findByUserId(user.getId()).stream()
                .map(AccountResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse createAccount(User user, AccountRequest request) {
        Account account = new Account();
        account.setUser(user);
        account.setName(request.getName());
        account.setType(request.getType());
        account.setBalance(request.getBalance() != null ? request.getBalance() : BigDecimal.ZERO);
        accountRepository.save(account);
        return new AccountResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(User user, UUID accountId, AccountRequest request) {
        Account account = getOwnedAccount(user, accountId);
        account.setName(request.getName());
        account.setType(request.getType());
        if (request.getBalance() != null) {
            account.setBalance(request.getBalance());
        }
        accountRepository.save(account);
        return new AccountResponse(account);
    }

    @Transactional
    public void deleteAccount(User user, UUID accountId) {
        Account account = getOwnedAccount(user, accountId);
        accountRepository.delete(account);
    }

    public Account getOwnedAccount(User user, UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta conta");
        }
        return account;
    }
}
