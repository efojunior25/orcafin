package com.orcafin.service;

import com.orcafin.dto.ParseTransactionResponse;
import com.orcafin.entity.Category;
import com.orcafin.entity.CategoryType;
import com.orcafin.entity.TransactionType;
import com.orcafin.entity.User;
import com.orcafin.exception.AiUnavailableException;
import com.orcafin.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiParsingService {

    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model}")
    private String ollamaModel;

    public ParseTransactionResponse parse(User user, String text) {
        return parse(user, text, null);
    }

    public ParseTransactionResponse parse(User user, String text, String transcribedText) {
        List<Category> categories = categoryRepository.findByUserIdOrUserIdIsNull(user.getId());

        String prompt = buildPrompt(text, categories);
        String rawResponse = callOllama(prompt);
        JsonNode parsed = extractJson(rawResponse);

        TransactionType type = parseType(parsed.path("type").asText("DESPESA"));
        BigDecimal amount = parseAmount(parsed.path("amount"));
        String description = parsed.path("description").asText(text).trim();
        String categoryNameGuess = parsed.path("categoryName").asText("");

        Category matched = matchCategory(categories, categoryNameGuess, type);

        return new ParseTransactionResponse(
                type,
                amount,
                description.isBlank() ? text : description,
                matched != null ? matched.getId() : null,
                matched != null ? matched.getName() : null,
                LocalDate.now(),
                transcribedText
        );
    }

    private String buildPrompt(String text, List<Category> categories) {
        String categoryList = categories.stream()
                .map(c -> "- " + c.getName() + " (" + c.getType() + ")")
                .reduce("", (a, b) -> a + b + "\n");

        return """
                Você extrai dados estruturados de um lançamento financeiro em português.
                Responda APENAS com um JSON válido, sem nenhum texto antes ou depois, no formato exato:
                {"type":"RECEITA ou DESPESA","amount":number,"description":"string curta","categoryName":"uma das categorias abaixo"}

                Categorias disponíveis:
                %s

                Regras:
                - Se o texto mencionar "recebi", "salário", "venda" etc, type é RECEITA. Caso contrário, DESPESA.
                - amount é sempre um número positivo (use ponto decimal, ex: 59.90).
                - description é um resumo curto do que foi comprado/recebido, sem o valor.
                - categoryName deve ser exatamente um dos nomes de categoria listados acima, escolhendo o mais adequado.

                Texto do usuário: "%s"
                """.formatted(categoryList, text);
    }

    private String callOllama(String prompt) {
        try {
            RestClient client = RestClient.create(ollamaBaseUrl);
            Map<String, Object> body = Map.of(
                    "model", ollamaModel,
                    "prompt", prompt,
                    "stream", false,
                    "format", "json"
            );
            Map<String, Object> response = client.post()
                    .uri("/api/generate")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("response") == null) {
                throw new AiUnavailableException("A IA local não retornou uma resposta válida.");
            }
            return response.get("response").toString();
        } catch (RestClientException e) {
            throw new AiUnavailableException(
                    "IA local indisponível. Verifique se o container 'orcafin-ollama' está rodando (docker compose --profile ai up -d ollama).");
        }
    }

    private JsonNode extractJson(String raw) {
        String cleaned = raw.trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new AiUnavailableException("A IA local respondeu em um formato inesperado.");
        }
        cleaned = cleaned.substring(start, end + 1);
        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            throw new AiUnavailableException("Não foi possível interpretar a resposta da IA local.");
        }
    }

    private TransactionType parseType(String value) {
        try {
            return TransactionType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return TransactionType.DESPESA;
        }
    }

    private BigDecimal parseAmount(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            throw new AiUnavailableException("Não consegui identificar um valor no texto. Tente algo como 'Pizza 59,90'.");
        }
        try {
            return new BigDecimal(node.asText()).abs();
        } catch (NumberFormatException e) {
            throw new AiUnavailableException("Não consegui identificar um valor no texto. Tente algo como 'Pizza 59,90'.");
        }
    }

    private Category matchCategory(List<Category> categories, String nameGuess, TransactionType type) {
        CategoryType categoryType = type == TransactionType.RECEITA ? CategoryType.RECEITA : CategoryType.DESPESA;

        return categories.stream()
                .filter(c -> c.getType() == categoryType)
                .filter(c -> c.getName().equalsIgnoreCase(nameGuess.trim()))
                .findFirst()
                .or(() -> categories.stream()
                        .filter(c -> c.getType() == categoryType && c.getName().equalsIgnoreCase("Outros"))
                        .findFirst())
                .orElse(null);
    }
}
