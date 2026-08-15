package com.orcafin.repository;

import com.orcafin.entity.Vault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VaultRepository extends JpaRepository<Vault, UUID> {
    List<Vault> findByUserId(UUID userId);
}
