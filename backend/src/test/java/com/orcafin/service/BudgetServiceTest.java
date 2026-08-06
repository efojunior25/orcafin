package com.orcafin.service;

import com.orcafin.dto.BudgetRequest;
import com.orcafin.entity.Budget;
import com.orcafin.entity.Category;
import com.orcafin.entity.CategoryType;
import com.orcafin.entity.PaymentMethod;
import com.orcafin.entity.Transaction;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.repository.BudgetRepository;
import com.orcafin.repository.CategoryRepository;
import com.orcafin.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private BudgetService budgetService;

    private User user;
    private Category alimentacao;
    private Category transporte;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetService(budgetRepository, categoryRepository, transactionRepository);
        user = new User();
        user.setId(UUID.randomUUID());

        alimentacao = new Category();
        alimentacao.setId(UUID.randomUUID());
        alimentacao.setType(CategoryType.DESPESA);

        transporte = new Category();
        transporte.setId(UUID.randomUUID());
        transporte.setType(CategoryType.DESPESA);
    }

    private Transaction despesa(Category category, String amount) {
        Transaction t = new Transaction();
        t.setType(TransactionType.DESPESA);
        t.setCategory(category);
        t.setAmount(new BigDecimal(amount));
        t.setPaymentMethod(PaymentMethod.DEBITO);
        return t;
    }

    @Test
    void categoryBudgetOnlyCountsMatchingCategory() {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setUser(user);
        budget.setCategory(alimentacao);
        budget.setLimitAmount(new BigDecimal("200.00"));

        when(budgetRepository.findByUserId(user.getId())).thenReturn(List.of(budget));
        when(transactionRepository.findByUserIdAndDateBetween(eq(user.getId()), any(), any()))
                .thenReturn(List.of(
                        despesa(alimentacao, "80.00"),
                        despesa(transporte, "500.00")
                ));

        var result = budgetService.listBudgets(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpentAmount()).isEqualByComparingTo("80.00");
        assertThat(result.get(0).getPercentage()).isEqualTo(40.0);
    }

    @Test
    void generalBudgetCountsAllDespesaCategories() {
        Budget budget = new Budget();
        budget.setId(UUID.randomUUID());
        budget.setUser(user);
        budget.setCategory(null);
        budget.setLimitAmount(new BigDecimal("100.00"));

        when(budgetRepository.findByUserId(user.getId())).thenReturn(List.of(budget));
        when(transactionRepository.findByUserIdAndDateBetween(eq(user.getId()), any(), any()))
                .thenReturn(List.of(
                        despesa(alimentacao, "80.00"),
                        despesa(transporte, "20.00")
                ));

        var result = budgetService.listBudgets(user);

        assertThat(result.get(0).getSpentAmount()).isEqualByComparingTo("100.00");
        assertThat(result.get(0).getPercentage()).isEqualTo(100.0);
    }

    @Test
    void creatingDuplicateCategoryBudgetIsRejected() {
        when(categoryRepository.findById(alimentacao.getId())).thenReturn(Optional.of(alimentacao));
        when(budgetRepository.findByUserIdAndCategoryId(user.getId(), alimentacao.getId()))
                .thenReturn(Optional.of(new Budget()));

        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(alimentacao.getId());
        request.setLimitAmount(new BigDecimal("50.00"));

        assertThatThrownBy(() -> budgetService.createBudget(user, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void creatingDuplicateGeneralBudgetIsRejected() {
        when(budgetRepository.findByUserIdAndCategoryIsNull(user.getId()))
                .thenReturn(Optional.of(new Budget()));

        BudgetRequest request = new BudgetRequest();
        request.setCategoryId(null);
        request.setLimitAmount(new BigDecimal("50.00"));

        assertThatThrownBy(() -> budgetService.createBudget(user, request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
