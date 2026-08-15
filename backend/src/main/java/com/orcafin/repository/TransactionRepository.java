package com.orcafin.repository;

import com.orcafin.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN FETCH t.account
            LEFT JOIN FETCH t.creditCard
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.destinationAccount
            WHERE t.user.id = :userId
            ORDER BY t.date DESC
            """)
    List<Transaction> findByUserIdOrderByDateDesc(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            LEFT JOIN FETCH t.account
            LEFT JOIN FETCH t.creditCard
            LEFT JOIN FETCH t.category
            LEFT JOIN FETCH t.destinationAccount
            WHERE t.user.id = :userId AND t.date BETWEEN :start AND :end
            ORDER BY t.date DESC
            """)
    List<Transaction> findByUserIdAndDateBetween(@Param("userId") UUID userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Transaction> findByCreditCardIdAndDateBetween(UUID creditCardId, LocalDate start, LocalDate end);

    List<Transaction> findByCreditCardIdAndDateAfter(UUID creditCardId, LocalDate date);

    boolean existsByPrepaidCardIdAndDateAndDescription(UUID prepaidCardId, LocalDate date, String description);
}
