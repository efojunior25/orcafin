package com.orcafin.repository;

import com.orcafin.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserIdOrderByDateDesc(UUID userId);
    List<Transaction> findByUserIdAndDateBetween(UUID userId, LocalDate start, LocalDate end);
    List<Transaction> findByCreditCardIdAndDateBetween(UUID creditCardId, LocalDate start, LocalDate end);
}
