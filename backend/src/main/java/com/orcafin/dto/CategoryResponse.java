package com.orcafin.dto;

import com.orcafin.entity.Category;
import com.orcafin.entity.CategoryType;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CategoryResponse {
    private final UUID id;
    private final String name;
    private final String icon;
    private final CategoryType type;
    private final boolean isDefault;

    public CategoryResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.icon = category.getIcon();
        this.type = category.getType();
        this.isDefault = category.isDefault();
    }
}
