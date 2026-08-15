package com.orcafin.repository;

import com.orcafin.entity.VaultMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VaultMovementRepository extends JpaRepository<VaultMovement, UUID> {
    List<VaultMovement> findByVaultIdOrderByDateDescCreatedAtDesc(UUID vaultId);
}
