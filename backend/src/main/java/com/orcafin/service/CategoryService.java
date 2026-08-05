package com.orcafin.service;

import com.orcafin.dto.CategoryRequest;
import com.orcafin.dto.CategoryResponse;
import com.orcafin.entity.Category;
import com.orcafin.entity.User;
import com.orcafin.exception.ForbiddenException;
import com.orcafin.exception.ResourceNotFoundException;
import com.orcafin.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> listCategories(User user) {
        return categoryRepository.findByUserIdOrUserIdIsNull(user.getId()).stream()
                .map(CategoryResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(User user, CategoryRequest request) {
        Category category = new Category();
        category.setUser(user);
        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setType(request.getType());
        category.setDefault(false);
        categoryRepository.save(category);
        return new CategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(User user, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        if (category.isDefault() || category.getUser() == null) {
            throw new ForbiddenException("Não é possível excluir uma categoria padrão");
        }
        if (!category.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta categoria");
        }
        categoryRepository.delete(category);
    }

    public Category getAccessible(User user, UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        boolean isOwn = category.getUser() != null && category.getUser().getId().equals(user.getId());
        if (!category.isDefault() && !isOwn) {
            throw new ForbiddenException("Você não tem acesso a esta categoria");
        }
        return category;
    }
}
