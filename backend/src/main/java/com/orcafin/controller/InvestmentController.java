package com.orcafin.controller;

import com.orcafin.dto.InvestmentRequest;
import com.orcafin.dto.InvestmentResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.InvestmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/investments")
@RequiredArgsConstructor
public class InvestmentController {

    private final InvestmentService investmentService;

    @GetMapping
    public ResponseEntity<?> list() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(investmentService.listInvestments(user));
    }

    @PostMapping
    public ResponseEntity<InvestmentResponse> create(@Valid @RequestBody InvestmentRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(investmentService.createInvestment(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvestmentResponse> update(@PathVariable UUID id, @Valid @RequestBody InvestmentRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(investmentService.updateInvestment(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        investmentService.deleteInvestment(user, id);
        return ResponseEntity.noContent().build();
    }
}
