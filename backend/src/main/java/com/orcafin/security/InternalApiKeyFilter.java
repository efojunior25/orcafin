package com.orcafin.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protege as rotas /api/internal/** (usadas pelo serviço whatsapp-bot) com uma
 * chave compartilhada simples, já que não é um usuário logado e não faz sentido
 * usar o fluxo de JWT aqui. As rotas são permitAll no Spring Security; a
 * autorização de verdade acontece neste filtro.
 */
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/api/internal/";

    @Value("${app.internal.api-key}")
    private String expectedKey;

    @PostConstruct
    void validateKey() {
        if (expectedKey == null || expectedKey.getBytes(StandardCharsets.UTF_8).length < 24) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY ausente ou curta demais (mínimo 24 bytes). Gere um valor forte, " +
                            "ex.: openssl rand -base64 32");
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER);
        if (providedKey == null || !constantTimeEquals(providedKey, expectedKey)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chave inválida");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
