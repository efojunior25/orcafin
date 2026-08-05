package com.orcafin.controller;

import com.orcafin.dto.CreditCardRequest;
import com.orcafin.dto.CreditCardResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.CreditCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final CreditCardService creditCardService;

    @GetMapping
    public ResponseEntity<?> list() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(creditCardService.listCreditCards(user));
    }

    @PostMapping
    public ResponseEntity<CreditCardResponse> create(@Valid @RequestBody CreditCardRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(creditCardService.createCreditCard(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CreditCardResponse> update(@PathVariable UUID id, @Valid @RequestBody CreditCardRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(creditCardService.updateCreditCard(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        creditCardService.deleteCreditCard(user, id);
        return ResponseEntity.noContent().build();
    }
}
