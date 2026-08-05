package com.orcafin.service;

import com.orcafin.dto.BudgetRequest;
import com.orcafin.dto.BudgetResponse;
import com.orcafin.entity.Budget;
import com.orcafin.entity.Category;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.BudgetRepository;
import com.orcafin.repository.CategoryRepository;
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
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public List<BudgetResponse> listBudgets(User user) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate from = currentMonth.atDay(1);
        LocalDate to = currentMonth.atEndOfMonth();
        List<Transaction> monthTransactions = transactionRepository.findByUserIdAndDateBetween(user.getId(), from, to);

        return budgetRepository.findByUserId(user.getId()).stream()
                .map(budget -> new BudgetResponse(budget, spentFor(budget, monthTransactions)))
                .collect(Collectors.toList());
    }

    private BigDecimal spentFor(Budget budget, List<Transaction> monthTransactions) {
        return monthTransactions.stream()
                .filter(t -> t.getType() == TransactionType.DESPESA)
                .filter(t -> budget.getCategory() == null
                        || (t.getCategory() != null && t.getCategory().getId().equals(budget.getCategory().getId())))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public BudgetResponse createBudget(User user, BudgetRequest request) {
        Category category = request.getCategoryId() != null ? getAccessibleCategory(user, request.getCategoryId()) : null;

        boolean exists = category == null
                ? budgetRepository.findByUserIdAndCategoryIsNull(user.getId()).isPresent()
                : budgetRepository.findByUserIdAndCategoryId(user.getId(), category.getId()).isPresent();
        if (exists) {
            throw new IllegalArgumentException("Já existe um orçamento para essa categoria");
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setLimitAmount(request.getLimitAmount());
        budgetRepository.save(budget);

        return new BudgetResponse(budget, BigDecimal.ZERO);
    }

    @Transactional
    public BudgetResponse updateBudget(User user, UUID budgetId, BudgetRequest request) {
        Budget budget = getOwnedBudget(user, budgetId);
        budget.setLimitAmount(request.getLimitAmount());
        budgetRepository.save(budget);

        YearMonth currentMonth = YearMonth.now();
        List<Transaction> monthTransactions = transactionRepository.findByUserIdAndDateBetween(
                user.getId(), currentMonth.atDay(1), currentMonth.atEndOfMonth());
        return new BudgetResponse(budget, spentFor(budget, monthTransactions));
    }

    @Transactional
    public void deleteBudget(User user, UUID budgetId) {
        Budget budget = getOwnedBudget(user, budgetId);
        budgetRepository.delete(budget);
    }

    private Budget getOwnedBudget(User user, UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Orçamento não encontrado"));
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a este orçamento");
        }
        return budget;
    }

    private Category getAccessibleCategory(User user, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        if (category.getUser() != null && !category.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta categoria");
        }
        return category;
    }
}
