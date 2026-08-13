package com.orcafin.service;

import com.orcafin.dto.Suggestion;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda em memória as sugestões geradas por uma importação de extrato, entre o
 * upload e a confirmação. Mesmo padrão de PendingWhatsappEntryService/RateLimiterService:
 * suficiente pra uma instância única, sem precisar de tabela nova no banco.
 */
@Service
public class ReconciliationSessionService {

    public record Session(UUID userId, UUID targetAccountId, UUID targetCreditCardId,
                           List<Suggestion> suggestions, Instant createdAt) {
    }

    private static final Duration TTL = Duration.ofMinutes(30);

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public UUID create(UUID userId, UUID targetAccountId, UUID targetCreditCardId, List<Suggestion> suggestions) {
        UUID sessionId = UUID.randomUUID();
        sessions.put(sessionId, new Session(userId, targetAccountId, targetCreditCardId, suggestions, Instant.now()));
        return sessionId;
    }

    public Optional<Session> get(UUID sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(session.createdAt().plus(TTL))) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void remove(UUID sessionId) {
        sessions.remove(sessionId);
    }
}
