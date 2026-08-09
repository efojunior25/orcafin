package com.orcafin.service;

import com.orcafin.dto.CreditCardRequest;
import com.orcafin.dto.CreditCardResponse;
import com.orcafin.entity.CreditCard;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.CreditCardRepository;
import com.orcafin.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final TransactionRepository transactionRepository;

    public List<CreditCardResponse> listCreditCards(User user) {
        return creditCardRepository.findByUserId(user.getId()).stream()
                .map(cc -> {
                    BigDecimal invoiceTotal = currentInvoiceTotal(cc);
                    return new CreditCardResponse(cc, invoiceTotal, usedLimit(cc, invoiceTotal));
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public CreditCardResponse createCreditCard(User user, CreditCardRequest request) {
        CreditCard creditCard = new CreditCard();
        creditCard.setUser(user);
        creditCard.setName(request.getName());
        creditCard.setCreditLimit(request.getCreditLimit());
        creditCard.setClosingDay(request.getClosingDay());
        creditCard.setDueDay(request.getDueDay());
        creditCardRepository.save(creditCard);
        return new CreditCardResponse(creditCard, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional
    public CreditCardResponse updateCreditCard(User user, UUID creditCardId, CreditCardRequest request) {
        CreditCard creditCard = getOwnedCreditCard(user, creditCardId);
        creditCard.setName(request.getName());
        creditCard.setCreditLimit(request.getCreditLimit());
        creditCard.setClosingDay(request.getClosingDay());
        creditCard.setDueDay(request.getDueDay());
        creditCardRepository.save(creditCard);
        return new CreditCardResponse(creditCard, currentInvoiceTotal(creditCard), usedLimit(creditCard));
    }

    /**
     * Limite consumido: fatura atual (ainda não fechada) + todas as parcelas/lançamentos
     * futuros já lançados no cartão. Faturas passadas não entram, pois o app não tem
     * conceito de "fatura paga" — presume-se que já foram quitadas fora daqui.
     */
    public BigDecimal usedLimit(CreditCard creditCard) {
        return usedLimit(creditCard, currentInvoiceTotal(creditCard));
    }

    private BigDecimal usedLimit(CreditCard creditCard, BigDecimal currentInvoiceTotal) {
        LocalDate today = LocalDate.now();
        LocalDate periodEnd = currentInvoicePeriod(creditCard, today)[1];

        BigDecimal total = currentInvoiceTotal;
        List<Transaction> future = transactionRepository.findByCreditCardIdAndDateAfter(creditCard.getId(), periodEnd);
        for (Transaction t : future) {
            if (t.getType() == TransactionType.DESPESA) {
                total = total.add(t.getAmount());
            } else if (t.getType() == TransactionType.RECEITA) {
                total = total.subtract(t.getAmount());
            }
        }
        return total;
    }

    @Transactional
    public void deleteCreditCard(User user, UUID creditCardId) {
        CreditCard creditCard = getOwnedCreditCard(user, creditCardId);
        creditCardRepository.delete(creditCard);
    }

    public CreditCard getOwnedCreditCard(User user, UUID creditCardId) {
        CreditCard creditCard = creditCardRepository.findById(creditCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Cartão de crédito não encontrado"));
        if (!creditCard.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a este cartão");
        }
        return creditCard;
    }

    private BigDecimal currentInvoiceTotal(CreditCard creditCard) {
        LocalDate today = LocalDate.now();
        LocalDate[] period = currentInvoicePeriod(creditCard, today);
        List<Transaction> transactions = transactionRepository
                .findByCreditCardIdAndDateBetween(creditCard.getId(), period[0], period[1]);

        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.DESPESA) {
                total = total.add(t.getAmount());
            } else if (t.getType() == TransactionType.RECEITA) {
                total = total.subtract(t.getAmount());
            }
        }
        return total;
    }

    private LocalDate[] currentInvoicePeriod(CreditCard creditCard, LocalDate today) {
        int closingDay = Math.min(creditCard.getClosingDay(), today.lengthOfMonth());
        LocalDate closingThisMonth = today.withDayOfMonth(closingDay);

        LocalDate periodEnd;
        if (!today.isAfter(closingThisMonth)) {
            periodEnd = closingThisMonth;
        } else {
            YearMonth nextMonth = YearMonth.from(today).plusMonths(1);
            periodEnd = nextMonth.atDay(Math.min(creditCard.getClosingDay(), nextMonth.lengthOfMonth()));
        }

        YearMonth periodStartMonth = YearMonth.from(periodEnd).minusMonths(1);
        LocalDate periodStart = periodStartMonth
                .atDay(Math.min(creditCard.getClosingDay(), periodStartMonth.lengthOfMonth()))
                .plusDays(1);

        return new LocalDate[]{periodStart, periodEnd};
    }
}
