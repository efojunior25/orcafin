package com.orcafin.controller;

import com.orcafin.dto.CategoryRequest;
import com.orcafin.dto.CategoryResponse;
import com.orcafin.entity.User;
import com.orcafin.security.SecurityUtils;
import com.orcafin.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> list() {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(categoryService.listCategories(user));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        User user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(categoryService.createCategory(user, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.getCurrentUser();
        categoryService.deleteCategory(user, id);
        return ResponseEntity.noContent().build();
    }
}
