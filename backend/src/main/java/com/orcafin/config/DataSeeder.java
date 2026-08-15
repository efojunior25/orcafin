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
            "Alimentação", "Transporte", "Transporte Público", "Moradia", "Saúde", "Educação",
            "Lazer", "Compras", "Contas e Serviços", "Tarifas e Juros", "Outros"
    );

    private static final List<String> RECEITA_CATEGORIES = List.of(
            "Salário", "Freelance", "Investimentos", "Rendimentos (Caixinha/Investimento)",
            "Presente", "Outros"
    );

    @Override
    public void run(String... args) {
        DESPESA_CATEGORIES.forEach(name -> ensureDefault(name, CategoryType.DESPESA));
        RECEITA_CATEGORIES.forEach(name -> ensureDefault(name, CategoryType.RECEITA));
    }

    /** Idempotente: só cria a categoria default se ainda não existir uma com o mesmo nome+tipo. */
    private void ensureDefault(String name, CategoryType type) {
        if (categoryRepository.findByIsDefaultTrueAndNameAndType(name, type).isPresent()) {
            return;
        }
        Category category = new Category();
        category.setUser(null);
        category.setName(name);
        category.setType(type);
        category.setDefault(true);
        categoryRepository.save(category);
    }
}
