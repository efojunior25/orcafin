package com.orcafin.config;

import com.orcafin.entity.Category;
import com.orcafin.entity.CategoryType;
import com.orcafin.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    private static final List<String> DESPESA_CATEGORIES = List.of(
            "Alimentação", "Transporte", "Moradia", "Saúde", "Educação",
            "Lazer", "Compras", "Contas e Serviços", "Outros"
    );

    private static final List<String> RECEITA_CATEGORIES = List.of(
            "Salário", "Freelance", "Investimentos", "Presente", "Outros"
    );

    @Override
    public void run(String... args) {
        if (!categoryRepository.findByIsDefaultTrue().isEmpty()) {
            return;
        }

        DESPESA_CATEGORIES.forEach(name -> saveDefault(name, CategoryType.DESPESA));
        RECEITA_CATEGORIES.forEach(name -> saveDefault(name, CategoryType.RECEITA));
    }

    private void saveDefault(String name, CategoryType type) {
        Category category = new Category();
        category.setUser(null);
        category.setName(name);
        category.setType(type);
        category.setDefault(true);
        categoryRepository.save(category);
    }
}
