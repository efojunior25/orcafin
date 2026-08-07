package com.orcafin.repository;

import com.orcafin.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, UUID> {
    List<LoginAudit> findByEmailOrderByCreatedAtDesc(String email, Pageable pageable);
}
