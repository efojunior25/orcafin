package com.orcafin.controller;

import com.orcafin.dto.AccountRequest;
import com.orcafin.dto.AccountResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<?> list() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(accountService.listAccounts(user));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(accountService.createAccount(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> update(@PathVariable UUID id, @Valid @RequestBody AccountRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(accountService.updateAccount(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        accountService.deleteAccount(user, id);
        return ResponseEntity.noContent().build();
    }
}
