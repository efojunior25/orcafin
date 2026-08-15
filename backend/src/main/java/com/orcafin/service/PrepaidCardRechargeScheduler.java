package com.orcafin.service;

import com.orcafin.entity.PaymentMethod;
import com.orcafin.entity.PrepaidCard;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.repository.PrepaidCardRepository;
import com.orcafin.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Credita automaticamente o saldo dos cartões pré-pagos com recarga configurada (ex: benefícios
 * recorrentes pagos pela empresa) no dia do mês definido em rechargeDay, lançando uma transação
 * RECEITA para manter o histórico. Idempotente: não relança se já existir recarga na mesma data.
 */
@Component
@RequiredArgsConstructor
public class PrepaidCardRechargeScheduler {

    private final PrepaidCardRepository prepaidCardRepository;
    private final TransactionRepository transactionRepository;

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void applyDailyRecharges() {
        LocalDate today = LocalDate.now();
        for (PrepaidCard card : prepaidCardRepository.findAll()) {
            if (card.getRechargeDay() == null || card.getRechargeAmount() == null) {
                continue;
            }
            if (card.getRechargeDay() != today.getDayOfMonth()) {
                continue;
            }
            if (card.getRechargeAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String description = "Recarga automática - " + card.getName();
            if (transactionRepository.existsByPrepaidCardIdAndDateAndDescription(card.getId(), today, description)) {
                continue;
            }

            card.setBalance(card.getBalance().add(card.getRechargeAmount()));
            prepaidCardRepository.save(card);

            Transaction transaction = new Transaction();
            transaction.setUser(card.getUser());
            transaction.setPrepaidCard(card);
            transaction.setType(TransactionType.RECEITA);
            transaction.setAmount(card.getRechargeAmount());
            transaction.setDescription(description);
            transaction.setDate(today);
            transaction.setPaymentMethod(PaymentMethod.TRANSFERENCIA);
            transactionRepository.save(transaction);
        }
    }
}
