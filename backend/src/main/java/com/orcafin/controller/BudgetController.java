package com.orcafin.controller;

import com.orcafin.dto.BudgetRequest;
import com.orcafin.dto.BudgetResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<?> list() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(budgetService.listBudgets(user));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@Valid @RequestBody BudgetRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(budgetService.createBudget(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(@PathVariable UUID id, @Valid @RequestBody BudgetRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(budgetService.updateBudget(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        budgetService.deleteBudget(user, id);
        return ResponseEntity.noContent().build();
    }
}
