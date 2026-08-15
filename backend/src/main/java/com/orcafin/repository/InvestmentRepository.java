package com.orcafin.repository;

import com.orcafin.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvestmentRepository extends JpaRepository<Investment, UUID> {
    List<Investment> findByUserId(UUID userId);
}
