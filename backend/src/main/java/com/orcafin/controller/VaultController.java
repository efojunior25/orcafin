package com.orcafin.controller;

import com.orcafin.dto.VaultMovementRequest;
import com.orcafin.dto.VaultRequest;
import com.orcafin.dto.VaultResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/vaults")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;

    @GetMapping
    public ResponseEntity<?> list() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(vaultService.listVaults(user));
    }

    @PostMapping
    public ResponseEntity<VaultResponse> create(@Valid @RequestBody VaultRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(vaultService.createVault(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VaultResponse> update(@PathVariable UUID id, @Valid @RequestBody VaultRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(vaultService.updateVault(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        vaultService.deleteVault(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<?> movements(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(vaultService.listMovements(user, id));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<VaultResponse> deposit(@PathVariable UUID id, @Valid @RequestBody VaultMovementRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(vaultService.deposit(user, id, request.getAmount()));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<VaultResponse> withdraw(@PathVariable UUID id, @Valid @RequestBody VaultMovementRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(vaultService.withdraw(user, id, request.getAmount()));
    }
}
