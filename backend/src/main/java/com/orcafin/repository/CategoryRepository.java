package com.orcafin.repository;

import com.orcafin.entity.Category;
import com.orcafin.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserIdOrUserIdIsNull(UUID userId);
    List<Category> findByIsDefaultTrue();
    Optional<Category> findByIsDefaultTrueAndNameAndType(String name, CategoryType type);
}
