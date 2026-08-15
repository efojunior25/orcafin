package com.orcafin.controller;

import com.orcafin.dto.PrepaidCardRequest;
import com.orcafin.dto.PrepaidCardResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.PrepaidCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/prepaid-cards")
@RequiredArgsConstructor
public class PrepaidCardController {

    private final PrepaidCardService prepaidCardService;

    @GetMapping
    public ResponseEntity<?> list() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(prepaidCardService.listPrepaidCards(user));
    }

    @PostMapping
    public ResponseEntity<PrepaidCardResponse> create(@Valid @RequestBody PrepaidCardRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(prepaidCardService.createPrepaidCard(user, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrepaidCardResponse> update(@PathVariable UUID id, @Valid @RequestBody PrepaidCardRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(prepaidCardService.updatePrepaidCard(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        prepaidCardService.deletePrepaidCard(user, id);
        return ResponseEntity.noContent().build();
    }
}
