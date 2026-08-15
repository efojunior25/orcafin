package com.orcafin.service;

import com.orcafin.dto.VaultRequest;
import com.orcafin.dto.VaultResponse;
import com.orcafin.entity.User;
import com.orcafin.entity.Vault;
import com.orcafin.entity.VaultMovement;
import com.orcafin.entity.VaultMovementType;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.VaultMovementRepository;
import com.orcafin.repository.VaultRepository;
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
public class VaultService {

    private final VaultRepository vaultRepository;
    private final VaultMovementRepository vaultMovementRepository;

    public List<VaultResponse> listVaults(User user) {
        return vaultRepository.findByUserId(user.getId()).stream()
                .map(VaultResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public VaultResponse createVault(User user, VaultRequest request) {
        Vault vault = new Vault();
        vault.setUser(user);
        vault.setName(request.getName());
        vault.setAnnualRate(request.getAnnualRate());
        vaultRepository.save(vault);
        return new VaultResponse(vault);
    }

    @Transactional
    public VaultResponse updateVault(User user, UUID vaultId, VaultRequest request) {
        Vault vault = getOwnedVault(user, vaultId);
        vault.setName(request.getName());
        vault.setAnnualRate(request.getAnnualRate());
        vaultRepository.save(vault);
        return new VaultResponse(vault);
    }

    @Transactional
    public void deleteVault(User user, UUID vaultId) {
        Vault vault = getOwnedVault(user, vaultId);
        vaultRepository.delete(vault);
    }

    public List<com.orcafin.dto.VaultMovementResponse> listMovements(User user, UUID vaultId) {
        Vault vault = getOwnedVault(user, vaultId);
        return vaultMovementRepository.findByVaultIdOrderByDateDescCreatedAtDesc(vault.getId()).stream()
                .map(com.orcafin.dto.VaultMovementResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public VaultResponse deposit(User user, UUID vaultId, BigDecimal amount) {
        Vault vault = getOwnedVault(user, vaultId);
        vault.setBalance(vault.getBalance().add(amount));
        vaultRepository.save(vault);
        recordMovement(vault, VaultMovementType.APORTE, amount);
        return new VaultResponse(vault);
    }

    @Transactional
    public VaultResponse withdraw(User user, UUID vaultId, BigDecimal amount) {
        Vault vault = getOwnedVault(user, vaultId);
        if (amount.compareTo(vault.getBalance()) > 0) {
            throw new IllegalArgumentException("Saldo insuficiente na caixinha");
        }
        vault.setBalance(vault.getBalance().subtract(amount));
        vaultRepository.save(vault);
        recordMovement(vault, VaultMovementType.RESGATE, amount);
        return new VaultResponse(vault);
    }

    private void recordMovement(Vault vault, VaultMovementType type, BigDecimal amount) {
        VaultMovement movement = new VaultMovement();
        movement.setVault(vault);
        movement.setType(type);
        movement.setAmount(amount);
        movement.setDate(LocalDate.now());
        vaultMovementRepository.save(movement);
    }

    public Vault getOwnedVault(User user, UUID vaultId) {
        Vault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new ResourceNotFoundException("Caixinha não encontrada"));
        if (!vault.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta caixinha");
        }
        return vault;
    }
}
